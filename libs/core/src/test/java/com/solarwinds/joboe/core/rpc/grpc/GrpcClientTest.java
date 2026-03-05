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

package com.solarwinds.joboe.core.rpc.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.protobuf.ByteString;
import com.solarwinds.joboe.core.rpc.Client;
import com.solarwinds.joboe.core.rpc.ClientFatalException;
import com.solarwinds.joboe.core.rpc.ProtocolClient;
import com.solarwinds.joboe.core.rpc.ProtocolClientFactory;
import com.solarwinds.joboe.core.rpc.ResultCode;
import com.solarwinds.joboe.core.rpc.RpcClient;
import com.solarwinds.joboe.core.rpc.RpcClient.TaskType;
import com.solarwinds.joboe.core.rpc.RpcClientTest;
import com.solarwinds.joboe.core.rpc.RpcSettings;
import com.solarwinds.joboe.core.settings.PollingSettingsFetcherTest;
import com.solarwinds.joboe.core.util.TimeUtils;
import com.solarwinds.joboe.sampling.SettingsArg;
import com.solarwinds.trace.ingestion.proto.Collector;
import com.solarwinds.trace.ingestion.proto.TraceCollectorGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import org.junit.jupiter.api.Test;

public class GrpcClientTest extends RpcClientTest {
  private static final String TEST_SERVER_PRIVATE_KEY_LOCATION =
      "src/test/java/com/solarwinds/joboe/core/rpc/grpc/test-collector-private.pem";

  private static final List<Collector.OboeSetting> TEST_OBOE_SETTINGS =
      convertToOboeSettings(TEST_SETTINGS);

  @SuppressWarnings("unchecked")
  private static List<Collector.OboeSetting> convertToOboeSettings(List<RpcSettings> settings) {
    List<Collector.OboeSetting> oboeSettings = new ArrayList<Collector.OboeSetting>();

    for (RpcSettings fromEntry : settings) {
      Map<String, ByteString> arguments = new HashMap<String, ByteString>();
      for (SettingsArg arg : SettingsArg.values()) {
        Object argValue = fromEntry.getArgValue(arg);
        if (argValue != null) {
          arguments.put(arg.getKey(), ByteString.copyFrom(arg.toByteBuffer(argValue)));
        }
      }
      Collector.OboeSetting setting =
          Collector.OboeSetting.newBuilder()
              .setFlags(ByteString.copyFromUtf8(PollingSettingsFetcherTest.DEFAULT_FLAGS_STRING))
              .setTimestamp(TimeUtils.getTimestampMicroSeconds())
              .setValue(1000000)
              .setTtl(600)
              .putAllArguments(arguments)
              .build();
      oboeSettings.add(setting);
    }
    return oboeSettings;
  }

  @Test
  public void testExhaustedServer() throws Exception {
    System.out.println("running testExhaustedServer");
    int exhaustedServerPort = locateAvailablePort();
    startExhaustedServer(exhaustedServerPort);
    Client client = null;

    try {
      client =
          new RpcClient(
              TEST_SERVER_HOST,
              exhaustedServerPort,
              TEST_CLIENT_ID,
              getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

      assertEquals(
          com.solarwinds.joboe.core.rpc.ResultCode.OK,
          client.postEvents(testEvents, null).get().getResultCode());
      fail("Expected " + ClientFatalException.class.getName() + " but found none");
    } catch (ExecutionException e) {
      if (!(e.getCause() instanceof ClientFatalException)) {
        fail("Expected " + ClientFatalException.class.getName() + " but found " + e.getCause());
      }
    } finally {
      if (client != null) {
        client.close();
      }
    }
  }

  @Override
  protected TestCollector startCollector(int port) throws IOException {
    return new GrpcCollector(port, new GrpcCollectorService());
  }

  @Override
  protected TestCollector startRedirectCollector(int port, String redirectArg) throws IOException {
    return new GrpcCollector(port, new GrpcRedirectCollectorService(redirectArg));
  }

  @Override
  protected TestCollector startRatedCollector(int port, ResultCode limitExceededCode)
      throws IOException {
    return new GrpcCollector(
        port,
        new GrpcRatedCollectorService(10, Collector.ResultCode.valueOf(limitExceededCode.name())));
  }

  @Override
  protected TestCollector startBiasedTestCollector(int port) throws IOException {
    return new GrpcCollector(
        port,
        new GrpcBiasedCollectorService(
            Collections.singletonMap(TaskType.POST_METRICS, Collector.ResultCode.TRY_LATER)));
  }

  @Override
  protected TestCollector startErroneousTestCollector(int port, double errorPercentage)
      throws IOException {
    return new GrpcCollector(port, new GrpcErroneousCollectorService(errorPercentage));
  }

  @Override
  protected void startSoftDisabledTestCollector(int port, String warning) throws IOException {
    new GrpcCollector(port, new GrpcCollectorService(Collector.ResultCode.OK, "", warning));
  }

  private void startExhaustedServer(int port) throws IOException {
    new GrpcCollector(
        port,
        new GrpcCollectorService() {
          @Override
          public void postEvents(
              Collector.MessageRequest request,
              StreamObserver<Collector.MessageResult> responseObserver) {
            responseObserver.onError(
                Status.RESOURCE_EXHAUSTED
                    .withDescription("Testing resource exhaustion on server side")
                    .asRuntimeException());
          }
        });
  }

  @Override
  protected ProtocolClientFactory<?> getProtocolClientFactory(URL certUrl)
      throws IOException, GeneralSecurityException {
    return new GrpcClient.GrpcProtocolClientFactory(certUrl);
  }

  private static class GrpcCollector implements TestCollector {
    private final Server server;
    private final GrpcCollectorService service;

    GrpcCollector(int port, GrpcCollectorService service) throws IOException {
      ServerBuilder builder =
          ServerBuilder.forPort(port)
              .useTransportSecurity(
                  new File(getServerPublicKeyLocation()),
                  new File(TEST_SERVER_PRIVATE_KEY_LOCATION))
              .maxInboundMessageSize(
                  ProtocolClient.MAX_CALL_SIZE + 1024 * 1024) // accept a slightly bigger message
              .addService(service);
      this.server = builder.build();
      this.service = service;
      server.start();
      System.out.println(
          "Grpc collector started at " + port + " with service GrpcCollectorService");
    }

    @Override
    public List<ByteBuffer> stop() {
      if (server != null) {
        server.shutdown();
        try {
          server.awaitTermination();
        } catch (InterruptedException ignore) {
        }
      }
      return flush();
    }

    @Override
    public List<ByteBuffer> flush() {
      return service.flush();
    }

    @Override
    public Map<TaskType, Long> getCallCountStats() {
      return service.getCallCountStats();
    }
  }

  private static class GrpcCollectorService extends TraceCollectorGrpc.TraceCollectorImplBase {
    private final Collector.ResultCode resultCode;
    private final String arg;
    private final String warning;
    protected List<ByteString> buffer =
        new ArrayList<ByteString>(); // what has been received so far
    protected final Collector.MessageResult result;
    private static final Collector.MessageResult PING_RESULT =
        Collector.MessageResult.newBuilder().setResult(Collector.ResultCode.OK).setArg("").build();
    @Getter private final Map<TaskType, Long> callCountStats = new HashMap<TaskType, Long>();

    public GrpcCollectorService() {
      this(Collector.ResultCode.OK, "", "");
    }

    public GrpcCollectorService(Collector.ResultCode resultCode, String arg, String warning) {
      this.resultCode = resultCode;
      this.arg = arg;
      this.warning = warning;
      this.result =
          Collector.MessageResult.newBuilder()
              .setResult(resultCode)
              .setArg(arg)
              .setWarning("")
              .build();
    }

    @Override
    public void postEvents(
        Collector.MessageRequest request,
        StreamObserver<Collector.MessageResult> responseObserver) {
      buffer.addAll(request.getMessagesList());
      responseObserver.onNext(result);
      responseObserver.onCompleted();

      incrementCallCountStats(TaskType.POST_EVENTS);
    }

    @Override
    public void postMetrics(
        Collector.MessageRequest request,
        StreamObserver<Collector.MessageResult> responseObserver) {
      buffer.addAll(request.getMessagesList());
      responseObserver.onNext(result);
      responseObserver.onCompleted();

      incrementCallCountStats(TaskType.POST_METRICS);
    }

    @Override
    public void postStatus(
        Collector.MessageRequest request,
        StreamObserver<Collector.MessageResult> responseObserver) {
      buffer.addAll(request.getMessagesList());
      responseObserver.onNext(result);
      responseObserver.onCompleted();

      incrementCallCountStats(TaskType.POST_STATUS);
    }

    @Override
    public void getSettings(
        Collector.SettingsRequest request,
        StreamObserver<Collector.SettingsResult> responseObserver) {
      Collector.SettingsResult settingsResult =
          Collector.SettingsResult.newBuilder()
              .setResult(resultCode)
              .setArg(arg)
              .setWarning(warning)
              .addAllSettings(TEST_OBOE_SETTINGS)
              .build();
      responseObserver.onNext(settingsResult);
      responseObserver.onCompleted();

      incrementCallCountStats(TaskType.GET_SETTINGS);
    }

    private void incrementCallCountStats(TaskType taskType) {
      Long existingCount = callCountStats.get(taskType);
      if (existingCount == null) {
        existingCount = 0L;
      }
      callCountStats.put(taskType, ++existingCount);
    }

    @Override
    public void ping(
        Collector.PingRequest request, StreamObserver<Collector.MessageResult> responseObserver) {
      responseObserver.onNext(PING_RESULT);
      responseObserver.onCompleted();
    }

    public List<ByteBuffer> flush() {
      List<ByteBuffer> messages = new ArrayList<>();

      for (ByteString entry : buffer) {
        messages.add(ByteBuffer.wrap(entry.toByteArray()));
      }

      buffer.clear();

      return messages;
    }
  }

  private static class GrpcBiasedCollectorService extends GrpcCollectorService {
    private final Map<TaskType, Collector.ResultCode> resultCodeByTaskType;

    public GrpcBiasedCollectorService(Map<TaskType, Collector.ResultCode> resultCodeByTaskType) {
      super();
      this.resultCodeByTaskType = resultCodeByTaskType;
    }

    @Override
    public void postEvents(
        Collector.MessageRequest request,
        StreamObserver<Collector.MessageResult> responseObserver) {
      if (resultCodeByTaskType.containsKey(TaskType.POST_EVENTS)) {
        responseObserver.onNext(
            Collector.MessageResult.newBuilder()
                .setResult(resultCodeByTaskType.get(TaskType.POST_EVENTS))
                .setArg("")
                .build());
        responseObserver.onCompleted();
      } else {
        super.postEvents(request, responseObserver);
      }
    }

    @Override
    public void postMetrics(
        Collector.MessageRequest request,
        StreamObserver<Collector.MessageResult> responseObserver) {
      if (resultCodeByTaskType.containsKey(TaskType.POST_METRICS)) {
        responseObserver.onNext(
            Collector.MessageResult.newBuilder()
                .setResult(resultCodeByTaskType.get(TaskType.POST_METRICS))
                .setArg("")
                .build());
        responseObserver.onCompleted();
      } else {
        super.postMetrics(request, responseObserver);
      }
    }

    @Override
    public void postStatus(
        Collector.MessageRequest request,
        StreamObserver<Collector.MessageResult> responseObserver) {
      if (resultCodeByTaskType.containsKey(TaskType.POST_STATUS)) {
        responseObserver.onNext(
            Collector.MessageResult.newBuilder()
                .setResult(resultCodeByTaskType.get(TaskType.POST_STATUS))
                .setArg("")
                .build());
        responseObserver.onCompleted();
      } else {
        super.postStatus(request, responseObserver);
      }
    }

    @Override
    public void getSettings(
        Collector.SettingsRequest request,
        StreamObserver<Collector.SettingsResult> responseObserver) {
      if (resultCodeByTaskType.containsKey(TaskType.GET_SETTINGS)) {
        responseObserver.onNext(
            Collector.SettingsResult.newBuilder()
                .setResult(resultCodeByTaskType.get(TaskType.GET_SETTINGS))
                .setArg("")
                .addAllSettings(TEST_OBOE_SETTINGS)
                .build());
        responseObserver.onCompleted();
      } else {
        super.getSettings(request, responseObserver);
      }
    }
  }

  private static class GrpcRedirectCollectorService extends GrpcCollectorService {
    private final Collector.MessageResult REDIRECT_RESULT;
    private final String redirectArg;

    public GrpcRedirectCollectorService(String redirectArg) {
      super();
      this.redirectArg = redirectArg;
      REDIRECT_RESULT =
          Collector.MessageResult.newBuilder()
              .setResult(Collector.ResultCode.REDIRECT)
              .setArg(redirectArg)
              .build();
    }

    @Override
    public void postEvents(
        Collector.MessageRequest request,
        StreamObserver<Collector.MessageResult> responseObserver) {
      responseObserver.onNext(REDIRECT_RESULT);
      responseObserver.onCompleted();
    }

    @Override
    public void postMetrics(
        Collector.MessageRequest request,
        StreamObserver<Collector.MessageResult> responseObserver) {
      responseObserver.onNext(REDIRECT_RESULT);
      responseObserver.onCompleted();
    }

    @Override
    public void postStatus(
        Collector.MessageRequest request,
        StreamObserver<Collector.MessageResult> responseObserver) {
      responseObserver.onNext(REDIRECT_RESULT);
      responseObserver.onCompleted();
    }

    @Override
    public void getSettings(
        Collector.SettingsRequest request,
        StreamObserver<Collector.SettingsResult> responseObserver) {
      Collector.SettingsResult settingsResult =
          Collector.SettingsResult.newBuilder()
              .setResult(Collector.ResultCode.REDIRECT)
              .setArg(redirectArg)
              .addAllSettings(TEST_OBOE_SETTINGS)
              .build();
      responseObserver.onNext(settingsResult);
      responseObserver.onCompleted();
    }

    @Override
    public void ping(
        Collector.PingRequest request, StreamObserver<Collector.MessageResult> responseObserver) {
      responseObserver.onNext(REDIRECT_RESULT);
      responseObserver.onCompleted();
    }
  }

  private static class GrpcRatedCollectorService extends GrpcCollectorService {
    private final int processingSpeedPerMessage;
    private final Collector.ResultCode fullResultCode;
    private final AtomicBoolean isProcessingAtomic = new AtomicBoolean(false);

    public GrpcRatedCollectorService(
        int processingTimePerMessage, Collector.ResultCode fullResultCode) {
      super();
      this.processingSpeedPerMessage = processingTimePerMessage;
      this.fullResultCode = fullResultCode;
    }

    @Override
    public void postEvents(
        Collector.MessageRequest request,
        StreamObserver<Collector.MessageResult> responseObserver) {
      processMessage(request.getMessagesList(), responseObserver);
    }

    @Override
    public void postMetrics(
        Collector.MessageRequest request,
        StreamObserver<Collector.MessageResult> responseObserver) {
      processMessage(request.getMessagesList(), responseObserver);
    }

    @Override
    public void postStatus(
        Collector.MessageRequest request,
        StreamObserver<Collector.MessageResult> responseObserver) {
      processMessage(request.getMessagesList(), responseObserver);
    }

    @Override
    public void getSettings(
        Collector.SettingsRequest request,
        StreamObserver<Collector.SettingsResult> responseObserver) {
      // do nothing
    }

    @Override
    public void ping(
        Collector.PingRequest request, StreamObserver<Collector.MessageResult> responseObserver) {
      processMessage(Collections.emptyList(), responseObserver);
    }

    @SuppressWarnings("unchecked")
    private void processMessage(final List<ByteString> messages, StreamObserver responseObserver) {
      boolean alreadyProcessing = isProcessingAtomic.getAndSet(true);
      if (alreadyProcessing) {
        responseObserver.onNext(
            Collector.MessageResult.newBuilder().setResult(fullResultCode).setArg("").build());
        responseObserver.onCompleted();
      } else {
        buffer.addAll(messages);

        // since the server is blocking, we have to return the result but delay setting back the
        // isProcessingAtomic flag to indicate that it's busy
        new Thread(
                () -> {
                  try {
                    Thread.sleep((long) messages.size() * processingSpeedPerMessage);
                  } catch (InterruptedException ignore) {
                  }
                  isProcessingAtomic.set(false);
                })
            .start();

        responseObserver.onNext(
            Collector.MessageResult.newBuilder()
                .setResult(Collector.ResultCode.OK)
                .setArg("")
                .build());
        responseObserver.onCompleted();
      }
    }
  }

  private static class GrpcErroneousCollectorService extends GrpcCollectorService {
    private final ErrorState errorState;

    public GrpcErroneousCollectorService(double errorPercentage) {
      super();
      this.errorState = new ErrorState(errorPercentage);
    }

    @Override
    public void postEvents(
        Collector.MessageRequest request,
        StreamObserver<Collector.MessageResult> responseObserver) {
      checkError();
      super.postEvents(request, responseObserver);
    }

    @Override
    public void postMetrics(
        Collector.MessageRequest request,
        StreamObserver<Collector.MessageResult> responseObserver) {
      checkError();
      super.postMetrics(request, responseObserver);
    }

    @Override
    public void postStatus(
        Collector.MessageRequest request,
        StreamObserver<Collector.MessageResult> responseObserver) {
      checkError();
      super.postStatus(request, responseObserver);
    }

    @Override
    public void getSettings(
        Collector.SettingsRequest request,
        StreamObserver<Collector.SettingsResult> responseObserver) {
      checkError();
      super.getSettings(request, responseObserver);
    }

    private void checkError() throws RuntimeException {
      if (errorState.isNextAsError()) {
        throw new RuntimeException("Test exception from erroneous server");
      }
    }
  }
}
