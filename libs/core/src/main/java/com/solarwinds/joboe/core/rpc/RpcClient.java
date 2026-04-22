/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solarwinds.joboe.core.rpc;

import com.solarwinds.joboe.core.Event;
import com.solarwinds.joboe.core.util.DaemonThreadFactory;
import com.solarwinds.joboe.core.util.HeartbeatSchedulerProvider;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.Logger.Level;
import com.solarwinds.joboe.logging.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.*;
import lombok.Getter;

/**
 * High level client that other code logic should use to make RPC calls to collector for various
 * operations such as posting events, getting trace settings.
 *
 * <p>This client takes care of connection management/retries automatically. The {@link ResultCode}
 * returned in the {@link Result} indicates the result of the "last attempt" before either a
 * successful response or all retries are exhausted.
 *
 * <p>For example if the final {@link ResultCode} is TRY_LATER, then that means this client has
 * already attempted to retry according to backup strategy as specified by {@link RetryParams} and
 * yet failed to get an OK response. Hence it returns the result of last attempt - TRY_LATER.
 *
 * <p>Take note that this client is protocol agnostic, the actual underlying protocol used in
 * determined by the {@link ProtocolClientFactory} provided in the constructor.
 */
public class RpcClient implements com.solarwinds.joboe.core.rpc.Client {
  private static final Logger logger = LoggerFactory.getLogger();

  protected String host;
  protected int port;
  private final String serviceKey;
  private boolean reportedConnectError = false;
  private boolean reportedRejectedExecutionError = false;
  private boolean isClosing =
      false; // indicates whether this current Collector client is closing permanently
  private Status connectionStatus = Status.NOT_CONNECTED;

  protected ProtocolClient protocolClient;
  private final ProtocolClientFactory<? extends ProtocolClient> protocolClientFactory;

  private static final int QUEUE_CAPACITY = 100;

  private static final int TIMEOUT = 10 * 1000; // 10 secs

  public enum TaskType {
    POST_EVENTS(true),
    POST_METRICS(true),
    POST_STATUS(true),
    GET_SETTINGS(true),
    CONNECTION_INIT(false);
    private final boolean threadpoolRequired;

    TaskType(boolean threadpoolRequired) {
      this.threadpoolRequired = threadpoolRequired;
    }
  }

  private final Map<TaskType, ExecutorService> services =
      new HashMap<
          RpcClient.TaskType,
          ExecutorService>(); // separate thread pool (single thread) for each message type, see
  // https://github.com/librato/joboe/issues/565

  private final RetryParamConstants
      defaultRetryParamConstants; // defines various retry param constants such as init delay, max
  // delay and max retry on various Result code

  private final HeartbeatScheduler heartbeatScheduler;

  /**
   * @param host host of the collector
   * @param port port of the collector
   * @param serviceKey serviceKey for this instrumented application (api token + service name)
   * @param protocolClientFactory factory used to instantiated the ProtocolClient
   * @param taskTypes taskTypes this client is expected to handle, if none is defined then this
   *     client will support all operations
   */
  public RpcClient(
      String host,
      int port,
      String serviceKey,
      ProtocolClientFactory<? extends ProtocolClient> protocolClientFactory,
      TaskType... taskTypes) {
    this(
        host, port, serviceKey, RetryParamConstants.getDefault(), protocolClientFactory, taskTypes);
  }

  /**
   * @param host host of the collector
   * @param port port of the collector
   * @param serviceKey serviceKey for this instrumented application (api token + service name)
   * @param retryParamConstants defines various retry param constants such as init delay, max delay
   *     and max retry on various ResultCode
   * @param protocolClientFactory factory used to instantiated the ProtocolClient
   * @param taskTypes taskTypes this client is expected to handle, if none is defined then this
   *     client will support all operations
   */
  public RpcClient(
      String host,
      int port,
      String serviceKey,
      RetryParamConstants retryParamConstants,
      ProtocolClientFactory<? extends ProtocolClient> protocolClientFactory,
      TaskType... taskTypes) {
    this.host = host;
    this.port = port;
    this.serviceKey = serviceKey;
    this.defaultRetryParamConstants = retryParamConstants;
    this.protocolClientFactory = protocolClientFactory;

    asyncInitClient();

    if (taskTypes == null || taskTypes.length == 0) {
      taskTypes = TaskType.values();
    }

    for (TaskType taskType : taskTypes) {
      if (taskType.threadpoolRequired) {
        services.put(
            taskType,
            new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(QUEUE_CAPACITY),
                DaemonThreadFactory.newInstance(taskType.name().toLowerCase() + "-executor")));
      }
    }

    heartbeatScheduler =
        HeartbeatSchedulerProvider.createHeartbeatScheduler(() -> protocolClient, serviceKey, this);
  }

  private <T extends Result> Future<T> submit(final Callable<T> clientCall, final TaskType taskType)
      throws RpcClientRejectedExecutionException {
    ExecutorService executor = services.get(taskType);
    if (executor == null) {
      throw new RpcClientRejectedExecutionException(
          "Cannot submit job of taskType ["
              + taskType
              + "] as this collector client only handles "
              + services.keySet());
    }

    FutureTask<T> task =
        new FutureTask<T>(
            new Callable<T>() {
              @Override
              public T call() throws ClientException {
                T result = null;

                RetryParams retryParams = new RetryParams(taskType);
                do {
                  result = handleClientCall(clientCall, retryParams);
                } while (!RpcClient.this.isClosing
                    && retryParams
                        .retry()); // retry on the retryParams (for example ResultCode == TRY_AGAIN
                // or connection recovered from failure)

                if (result == null) { // cannot even get a result object from the server
                  throw new ClientException(
                      "Failed to get response of taskType ["
                          + taskType
                          + "] from collector after "
                          + retryParams.currentRetryCounts
                          + " tries");
                }

                return result;
              }

              /**
               * Handles the actual collector call. This call blocks and modifies the underlying
               * protocol client connection if either connection exception arises or if a "redirect"
               * result is received. It is implemented this way as those conditions applies to the
               * any tasks submitted to this RpcClient instance hence blocking is required
               *
               * @param clientCall
               * @param retryParams
               * @return the result of the rpc call, take note that this could be null if the
               *     protocol client is reconnected during a failed call
               * @throws ClientException if fatal(unrecoverable) exception is found or if retry on
               *     failed connection has exceeded its limit
               */
              private T handleClientCall(Callable<T> clientCall, RetryParams retryParams)
                  throws ClientException {
                synchronized (
                    RpcClient.this) { // synchronize on the RpcClient instance as we do not want
                  // concurrent operation on the underlying protocol generated client
                  try {
                    if (!checkClient()) { // check/initialize the connection
                      return null;
                    }

                    T result = clientCall.call(); // actual call to the underlying protocol client

                    if (reportedConnectError) { // successfully made a call, if this connection had
                      // error previously, we should report that
                      // connection is recovered
                      logger.info(
                          "Protocol client ["
                              + taskType
                              + "] successfully recovered : "
                              + host
                              + ":"
                              + port);
                      reportedConnectError = false; // reset the flag
                    }
                    reportedRejectedExecutionError =
                        false; // successfully made a call, if any subsequent call is rejected due
                    // to full queue, it should print a new warning

                    if (result.getResultCode() == ResultCode.TRY_LATER) {
                      retryParams.flagRetry(RetryType.TRY_LATER);
                    } else if (result.getResultCode() == ResultCode.LIMIT_EXCEEDED) {
                      retryParams.flagRetry(RetryType.LIMIT_EXCEED);
                    } else if (result.getResultCode() == ResultCode.REDIRECT) {
                      if (retryParams.flagRetry(
                          RetryType.REDIRECT,
                          true)) { // flag retry on redirect, also clear other params
                        resetClient(
                            result.getArg()); // reset the client based on the redirect params
                      }
                    }

                    // update connection status
                    connectionStatus =
                        result.getResultCode().isError() ? Status.FAILURE : Status.OK;

                    heartbeatScheduler.schedule(); // connection is healthy, update keep alive

                    return result;
                  } catch (Exception e) {
                    if (RpcClient.this.isClosing) {
                      logger.debug(
                          "Found exception during collector Client shutdown. This is probably not critical as the client is shutting down : "
                              + e.getMessage(),
                          e);
                      return null;
                    } else if (e instanceof ClientRecoverableException) {
                      logConnectException(e, taskType);
                      reconnectClient(); // retry the connection, this blocks until connection is
                      // re-established
                      retryParams.flagRetry(RetryType.SERVER_ERROR); // retry after a server error
                      return null; // return null as the result as call result is unresolved
                    } else if (e instanceof ClientException) { // cannot recover
                      logger.warn(
                          "Error sending message to collector (fatal exception) ["
                              + taskType
                              + "] : "
                              + e.getClass().getName()
                              + " message: "
                              + e.getMessage()); // fatal exception so it's okay to be verbose
                      throw (ClientException) e; // re-throw it, does not make sense to retry
                    } else {
                      logger.warn(
                          "Error sending message to collector (fatal exception) ["
                              + taskType
                              + "] : "
                              + e.getClass().getName()
                              + " message: "
                              + e.getMessage()); // unexpected exception so it's okay to be verbose
                      throw new ClientException(e); // re-throw it, does not make sense to retry
                    }
                  }
                }
              }

              /**
               * Blocks until a connection is successfully re-established or max retry attempt is
               * reached
               */
              private void reconnectClient() {
                shutdownProtocolClient();
                initClient();
              }
            });

    try {
      executor.execute(task);
    } catch (RejectedExecutionException e) {
      if (!executor.isShutdown()) { // do not report exception if it's getting shutdown
        handleRejectedExecutionException(e);
        throw new RpcClientRejectedExecutionException(e);
      }
    }

    return task;
  }

  /**
   * Checks if underlying protocol client is available. Initialize the underlying client if it's not
   * yet available.
   *
   * <p>This method blocks according to the defaultRetryParamConstants. By default, this should
   * block indefinitely until connection is successfully initialized
   */
  private synchronized boolean checkClient() {
    if (protocolClient == null) {
      initClient();
    }
    return protocolClient != null;
  }

  /** Resets the underlying protocol outbound client based on the String arg (for host/port) */
  private void resetClient(String arg) throws ClientFatalException {
    shutdownProtocolClient(); // shut down the current generated client
    if (arg != null && !arg.isEmpty()) {
      String[] tokens = arg.split(":");

      String newHost;
      Integer newPort = null;
      if (tokens.length == 1) {
        logger.warn(
            "Redirect from Collector but couldn't locate port number from the response arg: ["
                + arg
                + "], using previous port: "
                + this.port);
        newHost = tokens[0];
      } else {
        newHost = tokens[0];
        try {
          newPort = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
          throw new ClientFatalException(
              "Failed to perform collector Redirect. Invalid port number ["
                  + tokens[1]
                  + "] found in arg ["
                  + arg
                  + "]");
        }
      }

      this.host = newHost;
      if (newPort != null) {
        this.port = newPort;
      }
      logger.info("Collector Redirect to " + this.host + ":" + this.port);

      initClient();
    } else {
      throw new ClientFatalException(
          "Failed to perform collector Redirect. Redirect args is empty");
    }
  }

  private void asyncInitClient() {
    ExecutorService executorService =
        Executors.newSingleThreadExecutor(DaemonThreadFactory.newInstance("init-rpc-client"));
    executorService.submit(() -> initClient());
    executorService.shutdown();
  }

  /**
   * Initialize the underlying protocol channel (create connection and perform a ping check)
   *
   * <p>It blocks and retries on failure unless max retry for CONNECTION_FAILURE is reached
   * according to defaultRetryParamConstants.
   */
  private synchronized void initClient() {
    RetryParams retryParams = new RetryParams(TaskType.CONNECTION_INIT);

    boolean initClient = !RpcClient.this.isClosing;
    while (initClient) {
      logger.debug("Creating collector client  : " + host + ":" + port);
      try {
        protocolClient = protocolClientFactory.buildClient(host, port);
        logger.debug("Created collector client  : " + host + ":" + port);

        connectionStatus = Status.OK;
        return;
      } catch (Exception e) {
        connectionStatus = Status.NOT_CONNECTED;
        if (retryParams.getCurrentRetryCount(RetryType.CONNECTION_FAILURE)
            == 20) { // prolonged outage
          logCriticalConnectException(e, TaskType.CONNECTION_INIT);
        } else {
          logConnectException(e, TaskType.CONNECTION_INIT);
        }

        retryParams.flagRetry(RetryType.CONNECTION_FAILURE);
        shutdownProtocolClient(); // always shuts down underlying and retry in this case
      }

      initClient = !RpcClient.this.isClosing && retryParams.retry();
    }
  }

  /**
   * Logs on every connection exception if applicable
   *
   * @param e
   * @param taskType
   */
  private void logConnectException(Exception e, TaskType taskType) {
    if (logger.shouldLog(
        Level.DEBUG)) { // if logging level is debug, then log this exception always
      logger.warn(
          "SSL client connection to collector ["
              + host
              + ":"
              + port
              + "] failed ["
              + taskType
              + "], message : "
              + e.getMessage(),
          e);
      reportedConnectError = true;
    }
  }

  /**
   * Logs on critical connection exception - for example prolonged connection init failure
   *
   * @param e
   * @param taskType
   */
  private void logCriticalConnectException(Exception e, TaskType taskType) {
    if (logger.shouldLog(Level.DEBUG)
        || !reportedConnectError) { // Warn it on the first time only for INFO+ logging settings
      if (logger.shouldLog(Level.DEBUG)) { // only report full trace if it's DEBUG logging settings
        logger.warn(
            "SSL client connection to collector ["
                + host
                + ":"
                + port
                + "] failed after retries ["
                + taskType
                + "], message : "
                + e.getMessage(),
            e);
      } else {
        logger.warn(
            "SSL client connection to collector ["
                + host
                + ":"
                + port
                + "] failed after retries ["
                + taskType
                + "], message : "
                + e.getMessage());
      }
      reportedConnectError = true;
    }
  }

  private void handleRejectedExecutionException(RejectedExecutionException e) {
    if (logger.shouldLog(Level.DEBUG)
        || !reportedRejectedExecutionError) { // Warn it on the first time only for INFO+ logging
      // settings
      logger.warn(
          "Rejected operation on Collector client side, probably due to full queue : "
              + e.getMessage());
      reportedRejectedExecutionError = true;
    }
  }

  /**
   * Posts tracing events to collector
   *
   * @param events tracing events to be posted
   * @param callback callback to invoke if the operation is completed, null means no callback
   * @return Future of the Result
   * @throws ClientException
   */
  @Override
  public Future<Result> postEvents(final List<Event> events, final Callback<Result> callback)
      throws ClientException {
    return submit(
        new CallableWithCallback<Result>(callback) {
          @Override
          public Result doCall() throws Exception {
            return protocolClient.doPostEvents(serviceKey, events);
          }
        },
        TaskType.POST_EVENTS);
  }

  /**
   * Posts metrics messages to collector
   *
   * @param messages metrics messages to be posted
   * @param callback callback to invoke if the operation is completed, null means no callback
   * @return Future of the Result
   * @throws ClientException
   */
  @Override
  public Future<Result> postMetrics(
      final List<Map<String, Object>> messages, final Callback<Result> callback)
      throws ClientException {
    return submit(
        new CallableWithCallback<Result>(callback) {
          @Override
          public Result doCall() throws Exception {
            return protocolClient.doPostMetrics(serviceKey, messages);
          }
        },
        TaskType.POST_METRICS);
  }

  /**
   * Posts status messages (for example init, framework usage) to collector
   *
   * @param messages status messages to be posted
   * @param callback callback to invoke if the operation is completed, null means no callback
   * @return Future of the Result
   * @throws ClientException
   */
  @Override
  public Future<Result> postStatus(
      final List<Map<String, Object>> messages, final Callback<Result> callback)
      throws ClientException {
    return submit(
        new CallableWithCallback<Result>(callback) {
          @Override
          public Result doCall() throws Exception {
            return protocolClient.doPostStatus(serviceKey, messages);
          }
        },
        TaskType.POST_STATUS);
  }

  /**
   * Gets tracing settings from the collector
   *
   * @param version version of the settings structure being requested
   * @param callback callback to invoke if the operation is completed, null means no callback
   * @return Future of the SettingsResult
   * @throws ClientException
   */
  @Override
  public Future<SettingsResult> getSettings(
      final String version, final Callback<SettingsResult> callback) throws ClientException {
    return submit(
        new CallableWithCallback<SettingsResult>(callback) {
          @Override
          public SettingsResult doCall() throws Exception {
            return protocolClient.doGetSettings(serviceKey, version);
          }
        },
        TaskType.GET_SETTINGS);
  }

  /**
   * Closes this high level rpc client and the underlying protocol client.
   *
   * <p>Stops accepting new rpc calls (new calls will be rejected with
   * RpcClientRejectedExecutionException) and finishes the remaining calls.
   *
   * <p>This client cannot be reused anymore after closing.
   */
  @Override
  public void close() {
    isClosing = true;

    // stop all the job queue executor services, but it should still process jobs that are queued
    for (ExecutorService service : services.values()) {
      if (connectionStatus != Status.OK) {
        logger.debug(
            "Force shutting down the collector client executor to avoid hanging due to connection retry");
        service.shutdownNow();
      } else {
        logger.debug("Shutting down the collector client executor");
        service.shutdown();
      }
    }

    shutdownProtocolClient();
  }

  private synchronized void shutdownProtocolClient() {
    if (protocolClient != null) {
      protocolClient.shutdown();
      protocolClient = null;
    }
  }

  /**
   * Gets the current connection {@link com.solarwinds.joboe.core.rpc.Client.Status}
   *
   * @return
   */
  @Override
  public Status getStatus() {
    return connectionStatus;
  }

  @Override
  public String toString() {
    return "RpcClient [host=" + host + ", port=" + port + "]";
  }

  private abstract static class CallableWithCallback<T extends Result> implements Callable<T> {
    private final Callback<T> callback;

    private CallableWithCallback(Callback<T> callback) {
      this.callback = callback;
    }

    private static String previousReportedWarning =
        null; // could be modified by multiple thread, but it's rather harmless even if that happens

    @Override
    public final T call() throws Exception {
      try {
        T result =
            doCall(); // might be more performant to separate the load generation with the actual
        // collector outbound call
        if (callback != null) {
          callback.complete(result);
        }

        String warning = result.getWarning();
        if (warning != null && !warning.isEmpty()) {
          if (logger.shouldLog(Level.DEBUG) || !warning.equals(previousReportedWarning)) {
            logger.warn("RPC call warning : [" + warning + "]");
            previousReportedWarning = warning;
          }
        } else if (result.getResultCode()
            == ResultCode.OK) { // then reset the previous warning, since it's OK now
          previousReportedWarning = null;
        }

        return result;
      } catch (Exception e) {
        if (callback != null) {
          callback.fail(e);
        }
        throw e;
      }
    }

    public abstract T doCall() throws Exception;
  }

  /**
   * Identifies reasoning for a retry operation
   *
   * @author pluk
   */
  @Getter
  enum RetryType {
    TRY_LATER(true), // retry as result code is TRY_LATER
    LIMIT_EXCEED(true), // retry as result code is LIMIT_EXCEED
    CONNECTION_FAILURE(true), // retry due to failed connection
    SERVER_ERROR(true), // retry after a error triggered by server
    CONNECTION_RECOVERED(false), // retry due to connection successfully recovered from failure
    REDIRECT(false); // retry as result code is REDIRECT

    /**
     * -- GETTER --
     *
     * @return whether this result code is considered a "failure" result code
     */
    private final boolean failure;

    RetryType(boolean failure) {
      this.failure = failure;
    }
  }

  /** Default values for retry params */
  public static class RetryParamConstants {
    private final Map<TaskType, Map<RetryType, Integer>> initRetryDelaysByTaskType =
        new HashMap<TaskType, Map<RetryType, Integer>>();
    private final Map<TaskType, Map<RetryType, Integer>> maxRetryDelaysByTaskType =
        new HashMap<TaskType, Map<RetryType, Integer>>();
    private final Map<TaskType, Map<RetryType, Integer>> maxRetryCountsByTaskType =
        new HashMap<TaskType, Map<RetryType, Integer>>();

    private static final int DEFAULT_INIT_DELAY = 500;
    private static final int DEFAULT_MAX_DELAY = 60000;
    private static final int DEAFULT_MAX_RETRY_COUNT = 20;

    /**
     * Retrieves the default param constants, which has init delay 500 ms, max delay 60000 ms, and
     * max retry count 20.
     *
     * <p>Take note that for task type "GET_SETTING", the max retry count is set to 0 on failure
     * RetryType (no retry on failure RetryType)
     */
    static RetryParamConstants getDefault() {

      return new RetryParamConstants(
          DEFAULT_INIT_DELAY, DEFAULT_MAX_DELAY, DEAFULT_MAX_RETRY_COUNT);
    }

    private final void setMaxRetryCountsOnFailure(TaskType taskType, Integer maxRetryCount) {
      for (Entry<RetryType, Integer> maxRetryCountEntry :
          maxRetryCountsByTaskType.get(taskType).entrySet()) {
        if (maxRetryCountEntry.getKey().isFailure()) {
          maxRetryCountEntry.setValue(maxRetryCount);
        }
      }
    }

    /** for internal testing purposes */
    public RetryParamConstants(int initRetryDelay, int maxRetryDelay, int maxRetryCount) {
      for (TaskType taskType : TaskType.values()) {
        Map<RetryType, Integer> initRetryDelays = new HashMap<>();
        initRetryDelays.put(RetryType.TRY_LATER, initRetryDelay);
        initRetryDelays.put(RetryType.LIMIT_EXCEED, initRetryDelay);
        initRetryDelays.put(RetryType.CONNECTION_FAILURE, initRetryDelay);
        initRetryDelays.put(RetryType.SERVER_ERROR, initRetryDelay);
        initRetryDelaysByTaskType.put(taskType, initRetryDelays);

        Map<RetryType, Integer> maxRetryDelays = new HashMap<>();
        maxRetryDelays.put(RetryType.TRY_LATER, maxRetryDelay);
        maxRetryDelays.put(RetryType.LIMIT_EXCEED, maxRetryDelay);
        maxRetryDelays.put(RetryType.CONNECTION_FAILURE, maxRetryDelay);
        maxRetryDelays.put(RetryType.SERVER_ERROR, maxRetryDelay);
        maxRetryDelaysByTaskType.put(taskType, maxRetryDelays);

        Map<RetryType, Integer> maxRetryCounts = new HashMap<>();
        maxRetryCounts.put(RetryType.TRY_LATER, maxRetryCount);
        maxRetryCounts.put(RetryType.LIMIT_EXCEED, maxRetryCount);
        maxRetryCounts.put(
            RetryType.CONNECTION_FAILURE,
            null); // CONNECITON_FAILURE - it should always retry indefinitely, unless specified
        // otherwise
        maxRetryCounts.put(RetryType.SERVER_ERROR, maxRetryCount);
        maxRetryCounts.put(RetryType.REDIRECT, maxRetryCount);
        maxRetryCountsByTaskType.put(taskType, maxRetryCounts);
      }

      setMaxRetryCountsOnFailure(
          TaskType.GET_SETTINGS,
          0); // special case...no retry for GET_SETTINGS on failure RetryType
      setMaxRetryCountsOnFailure(
          TaskType.CONNECTION_INIT,
          null); // special case. for CONNECTION_INIT, we keep retrying until it's successful
    }
  }

  /**
   * Represents the state of retrying. Provides ways to flag retry and performs sleep based {@link
   * RetryType}
   */
  class RetryParams {
    private static final double RETRY_MULTIPLIER = 1.5;
    private final Map<RetryType, Integer> currentRetryDelays = new HashMap<RetryType, Integer>();
    private final Map<RetryType, Integer> currentRetryCounts = new HashMap<RetryType, Integer>();

    private boolean shouldRetry;
    private Integer activeDelay;
    private final TaskType taskType;

    RetryParams(TaskType taskType) {
      this.taskType = taskType;
    }

    /**
     * Flags for a retry with a given reason in the {@link RetryType}, this will affect the outcome
     * of the next {@link RetryParams#retry()} call.
     *
     * <p>Take note that flagging a retry with this method alone does not determine the final
     * outcome of the next {@link RetryParams#retry()} call, the current states of the <code>
     * RetryParams</code> would also be assessed (for example if the current retry count has
     * exceeded the limit)
     *
     * @param retryType
     * @return whether flagging is successful based on the current state
     */
    public boolean flagRetry(RetryType retryType) {
      return flagRetry(retryType, false);
    }

    /**
     * Flags for a retry with a given reason in the {@link RetryType}, this will affect the outcome
     * of the next {@link RetryParams#retry()} call.
     *
     * <p>Take note that flagging a retry with this method alone does not determine the final
     * outcome of the next {@link RetryParams#retry()} call, the current states of the <code>
     * RetryParams</code> would also be assessed (for example if the current retry count has
     * exceeded the limit)
     *
     * @param retryType
     * @param resetOtherParams whether to reset the states of all params with other <code>RetryType
     *     </code>
     * @return whether flagging is successful based on the current state
     */
    public boolean flagRetry(RetryType retryType, boolean resetOtherParams) {
      // first increment delay
      Integer delay = currentRetryDelays.get(retryType);
      if (delay == null) {
        delay = getInitRetryDelay(taskType, retryType);
        if (delay == null) {
          delay = 0;
        }
      } else {
        Integer maxRetryDelay = getMaxRetryDelay(taskType, retryType);
        delay =
            maxRetryDelay != null
                ? Math.min((int) (delay * RETRY_MULTIPLIER), maxRetryDelay)
                : (int) (delay * RETRY_MULTIPLIER);
      }

      if (resetOtherParams) {
        currentRetryDelays.clear();
      }

      currentRetryDelays.put(retryType, delay);

      // then increment count
      Integer retryCount = currentRetryCounts.get(retryType);
      if (retryCount == null) {
        retryCount = 1;
      } else {
        retryCount++;
      }

      if (resetOtherParams) {
        currentRetryCounts.clear();
      }

      currentRetryCounts.put(retryType, retryCount);

      // update the should retry flag
      Integer maxRetryCount =
          getMaxRetryCount(taskType, retryType); // could be null if there's no limit to it
      shouldRetry = maxRetryCount == null || retryCount <= maxRetryCount;
      if (!shouldRetry) {
        logger.debug(
            "Not going to retry message as max retry count ["
                + maxRetryCount
                + "] is exceeded, cause: "
                + retryType);
        activeDelay = 0;
      } else {
        logger.debug(
            "Flagging to retry message with delay [" + delay + "] ms, cause: " + retryType);
        activeDelay = delay;
      }

      return shouldRetry;
    }

    private Integer getMaxRetryCount(TaskType taskType, RetryType retryType) {
      return defaultRetryParamConstants.maxRetryCountsByTaskType.get(taskType).get(retryType);
    }

    private Integer getMaxRetryDelay(TaskType taskType, RetryType retryType) {
      return defaultRetryParamConstants.maxRetryDelaysByTaskType.get(taskType).get(retryType);
    }

    private Integer getInitRetryDelay(TaskType taskType, RetryType retryType) {
      return defaultRetryParamConstants.initRetryDelaysByTaskType.get(taskType).get(retryType);
    }

    private int getCurrentRetryCount(RetryType retryType) {
      return currentRetryCounts.containsKey(retryType) ? currentRetryCounts.get(retryType) : 0;
    }

    /**
     * Determines whether a retry should be performed based on previous call of {@link
     * RetryParams#flagRetry(RetryType) } with consideration of current states for retry
     * restrictions on the "TaskType" of this RetryParam refers to.
     *
     * <p>This might enforce time wait (thread sleep) if a retry should be performed with delay
     *
     * @return whether retry should be performed
     */
    public boolean retry() {
      if (shouldRetry) {
        try {
          if (activeDelay > 0) {
            logger.debug("Collector client retry sleeping for " + activeDelay + " millisecs");
            TimeUnit.MILLISECONDS.sleep(activeDelay);
          }
        } catch (InterruptedException e) {
          logger.debug("Collector client retry sleep is interrupted, message: " + e.getMessage());
          return false; // should not retry if sleep is interrupted
        } finally {
          activeDelay = 0;
          shouldRetry = false; // reset
        }
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * A keep alive monitor that sends a ping message if it has been idle for 20 seconds
   *
   * @author pluk
   */
}
