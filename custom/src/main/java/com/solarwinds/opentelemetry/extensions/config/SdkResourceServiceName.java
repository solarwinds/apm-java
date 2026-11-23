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

package com.solarwinds.opentelemetry.extensions.config;

import com.solarwinds.joboe.logging.LoggerFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Holds the final {@code service.name} of the built SDK's resource, published by the agent listener
 * once the SDK is built. The settings pollers are created while the SDK is being built, so they
 * wait for the name to be published.
 */
public final class SdkResourceServiceName {
  private static final long DEFAULT_WAIT_SECONDS = 10;

  private static volatile CompletableFuture<String> serviceName = new CompletableFuture<>();

  private static volatile boolean timedOut;

  private SdkResourceServiceName() {}

  public static void set(String name) {
    serviceName.complete(name);
  }

  /** Returns the published service name, waiting up to 10 seconds on the first call. */
  public static String get() {
    return get(DEFAULT_WAIT_SECONDS, TimeUnit.SECONDS);
  }

  /**
   * Returns the published service name, waiting up to the timeout on the first call. Once a wait
   * has timed out, later calls return immediately.
   *
   * @return the service name, or null if it's not available
   */
  public static String get(long timeout, TimeUnit unit) {
    CompletableFuture<String> current = serviceName;
    if (timedOut) {
      return current.getNow(null);
    }

    try {
      return current.get(timeout, unit);
    } catch (TimeoutException e) {
      timedOut = true;
      LoggerFactory.getLogger()
          .warn(
              "Timed out waiting for the SDK resource service name, falling back to the service"
                  + " key or default service name");
      return null;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  /** For testing only. */
  public static void reset() {
    serviceName = new CompletableFuture<>();
    timedOut = false;
  }
}
