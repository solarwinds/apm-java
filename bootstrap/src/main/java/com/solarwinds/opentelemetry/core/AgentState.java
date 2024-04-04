package com.solarwinds.opentelemetry.core;

import com.solarwinds.joboe.logging.LoggerFactory;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class AgentState {
  public static boolean waitForReady(long timeout, TimeUnit unit) {
    try {
      if (startupTasksFuture.get() != null) {
        startupTasksFuture.get().get(timeout, unit);
        return true;
      } else {
        throw new IllegalStateException("startupTasksFuture is not yet initialized!");
      }
    } catch (IllegalStateException
        | InterruptedException
        | ExecutionException
        | TimeoutException exception) {
      LoggerFactory.getLogger()
          .error("Error waiting for agent to finish initialization", exception);
    }
    return false;
  }

  public static void setStartupTasksFuture(Future<?> future) {
    startupTasksFuture.set(future);
  }

  private static final AtomicReference<Future<?>> startupTasksFuture = new AtomicReference<>();
}
