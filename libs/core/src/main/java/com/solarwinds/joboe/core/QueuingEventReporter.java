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

package com.solarwinds.joboe.core;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.core.rpc.Client;
import com.solarwinds.joboe.core.rpc.ClientLoggingCallback;
import com.solarwinds.joboe.core.rpc.Result;
import com.solarwinds.joboe.core.rpc.ResultCode;
import com.solarwinds.joboe.core.util.DaemonThreadFactory;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.SettingsArg;
import com.solarwinds.joboe.sampling.SettingsArgChangeListener;
import com.solarwinds.joboe.sampling.SettingsManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A reporter that accepts events into a queue w/o blocking but sends out events synchronously.
 *
 * <p>Accepts events are inserted into a queue and being consumed and sent out synchronously. If
 * sending rate is slower than the queuing rate, then this report will attempt to send out events in
 * batches
 *
 * @author pluk
 */
public class QueuingEventReporter implements EventReporter {
  static final int QUEUE_CAPACITY = 10000;
  static final int SEND_CAPACITY;
  private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>(QUEUE_CAPACITY);
  protected ExecutorService executorService =
      Executors.newSingleThreadExecutor(DaemonThreadFactory.newInstance("queuing-event-reporter"));

  private final ClientLoggingCallback<Result> loggingCallback =
      new ClientLoggingCallback<Result>("send events");

  protected static Logger logger = LoggerFactory.getLogger();
  private final AtomicEventReporterStats stats = new AtomicEventReporterStats(eventQueue::size);

  private static final long REPORT_QUEUE_FULL_INTERVAL = 60 * 1000; // 1 minute
  static final int DEFAULT_FLUSH_INTERVAL = 2; // 2 second

  static int flushInterval = DEFAULT_FLUSH_INTERVAL; // in unit of second

  static {
    if (ConfigManager.getConfig(ConfigProperty.AGENT_EVENTS_FLUSH_INTERVAL) instanceof Integer) {
      setFlushInterval(
          (Integer) ConfigManager.getConfig(ConfigProperty.AGENT_EVENTS_FLUSH_INTERVAL));
    }
    SettingsManager.registerListener(
        new SettingsArgChangeListener<Integer>(SettingsArg.EVENTS_FLUSH_INTERVAL) {
          @Override
          public void onChange(Integer newValue) {
            if (newValue != null) {
              setFlushInterval(newValue);
            } else { // reset to default
              setFlushInterval(DEFAULT_FLUSH_INTERVAL);
            }
          }
        });

    SEND_CAPACITY =
        ConfigManager.getConfigOptional(ConfigProperty.AGENT_EVENTS_SEND_CAPACITY, 1000);
  }

  private boolean reportedQueueFull = false;
  private long reportedQueueFullTime = 0;

  private final SendRunnable sendRunnable;

  protected final Client client;

  static void setFlushInterval(int eventsFlushInterval) {
    if (eventsFlushInterval >= 0) {
      flushInterval = eventsFlushInterval;
      logger.debug("Event flush interval set to " + eventsFlushInterval + "s");
    } else {
      logger.warn("Event flush interval value " + eventsFlushInterval + " is not valid");
    }
  }

  public QueuingEventReporter(Client client) {
    this.client = client;
    sendRunnable = new SendRunnable();
    executorService.execute(sendRunnable);
  }

  private class SendRunnable implements Runnable {
    private boolean exitSignalled = false;
    private CountDownLatch countDownLatch = new CountDownLatch(1);
    private final ScheduledExecutorService scheduledExecutorService =
        Executors.newScheduledThreadPool(1, DaemonThreadFactory.newInstance("send-event-delay"));

    @Override
    public void run() {
      while (!exitSignalled || !eventQueue.isEmpty()) {
        List<Event> sendingEvents = new ArrayList<Event>();
        try {
          sendingEvents.add(
              eventQueue.take()); // this blocks until at least one event is available;

          eventQueue.drainTo(
              sendingEvents, SEND_CAPACITY - sendingEvents.size()); // drain the rest of the queue

          // sleep a while to batch up events, but only sleep if there's no build up, if the
          // sendingEvents reaches capacity, we should send events right the way
          if (sendingEvents.size() < SEND_CAPACITY) {
            try {
              waitForNextSend();
            } catch (InterruptedException e) {
              logger.debug("Queuing event wait interrupted");
            }
            eventQueue.drainTo(
                sendingEvents, SEND_CAPACITY - sendingEvents.size()); // try draining again
          }

          Result result = synchronousSend(sendingEvents);
          ResultCode resultCode = result.getResultCode();

          if (resultCode.isError()) {
            logger.debug("Failed to send out " + sendingEvents.size() + " events");
            stats.incrementFailedCount(sendingEvents.size());
          } else {
            stats.incrementSentCount(sendingEvents.size());
          }

        } catch (Exception e) {
          // do not retry the message, just log the problem
          logger.debug(
              "Failed to send "
                  + sendingEvents.size()
                  + " events, exception found: "
                  + e.getMessage()); // Should not be too noisy
          stats.incrementFailedCount(sendingEvents.size());
        } finally {
          stats.incrementProcessedCount(sendingEvents.size());
        }
      }
      scheduledExecutorService.shutdownNow();
    }

    /** Signals sending out events immediately */
    protected void sendNow() {
      if (countDownLatch != null && countDownLatch.getCount() > 0) {
        logger.debug("SendNow signaled for event reporter");
        countDownLatch.countDown();
      }
    }

    /** Blocks until the flushInterval elapsed or sendNow is signaled */
    private void waitForNextSend() throws InterruptedException {
      countDownLatch = new CountDownLatch(1);
      scheduledExecutorService.schedule(
          () -> countDownLatch.countDown(), flushInterval, TimeUnit.SECONDS);
      countDownLatch.await();
    }
  }

  /**
   * Signals to sends an event via this reporter. Take note that the actual outbound request might
   * be sent later on
   *
   * @throws EventReporterException if the reporter cannot accepts this event, for example the queue
   *     is full
   */
  @Override
  public void send(Event event) throws EventReporterException {
    if (!eventQueue.offer(event)) {
      stats.incrementOverflowedCount(1);
      stats.incrementProcessedCount(1);

      if (!reportedQueueFull) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - reportedQueueFullTime
            >= REPORT_QUEUE_FULL_INTERVAL) { // at most once every minute
          logger.warn(
              "Fail to report tracing event as the event queue is full in the reporter"); // only
          // print
          // warning
          // on
          // first
          // queue
          // overflow
          reportedQueueFull = true;
          reportedQueueFullTime = currentTime;
        }
      }

      throw new EventReporterQueueFullException("Cannot send event as the reporter queue is full");
    } else {
      stats.setQueueCount(eventQueue.size());

      if (eventQueue.size()
          >= SEND_CAPACITY) { // should start sending early (if sleeping) as it's filling up
        sendRunnable.sendNow();
      }

      reportedQueueFull = false;
    }
  }

  public void flush() {
    sendRunnable.sendNow();
  }

  /**
   * Gets and clears the current reporter stats
   *
   * @return
   */
  @Override
  public EventReporterStats consumeStats() {
    return stats.consumeStats();
  }

  /**
   * Closes this reporter orderly. Allow all submitted events to be processed with a default timeout
   */
  @Override
  public void close() {
    logger.debug("Closing queueing event reporter, signaling shut down after sending all events");
    if (client.getStatus() != Client.Status.OK) {
      logger.debug("RPC client is not OK. Shutting down the service now");
      executorService.shutdownNow();
    } else {
      executorService.shutdown();
    }

    sendRunnable.exitSignalled = true;
    try {
      client.close();
      boolean termination = executorService.awaitTermination(5, TimeUnit.SECONDS);
      logger.debug(String.format("Event reporter service shut down: [%s]", termination));
    } catch (InterruptedException ignore) {
    }
  }

  /**
   * This should only return upon completion of sending all the provided events (success or failure)
   *
   * @param events
   * @return
   * @throws Exception
   */
  public Result synchronousSend(List<Event> events) throws Exception {
    return client
        .postEvents(events, loggingCallback)
        .get(); // block until the client has finished the request
  }
}
