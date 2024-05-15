/*
 * Copyright SolarWinds Worldwide, LLC.
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
