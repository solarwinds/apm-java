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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SdkResourceServiceNameTest {

  @AfterEach
  void tearDown() {
    SdkResourceServiceName.reset();
  }

  @Test
  void returnsPublishedServiceName() {
    SdkResourceServiceName.set("sdk-service");

    assertEquals("sdk-service", SdkResourceServiceName.get(0, TimeUnit.SECONDS));
  }

  @Test
  void returnsServiceNamePublishedWhileWaiting() {
    new Thread(() -> SdkResourceServiceName.set("sdk-service")).start();

    assertEquals("sdk-service", SdkResourceServiceName.get(5, TimeUnit.SECONDS));
  }

  @Test
  void returnsNullWhenPublishedServiceNameIsNull() {
    SdkResourceServiceName.set(null);

    assertNull(SdkResourceServiceName.get(5, TimeUnit.SECONDS));
  }

  @Test
  void stopsWaitingAfterTimeout() {
    assertNull(SdkResourceServiceName.get(10, TimeUnit.MILLISECONDS));

    long start = System.nanoTime();
    assertNull(SdkResourceServiceName.get(5, TimeUnit.SECONDS));
    assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) < 1);
  }

  @Test
  void returnsServiceNamePublishedAfterTimeout() {
    assertNull(SdkResourceServiceName.get(10, TimeUnit.MILLISECONDS));
    SdkResourceServiceName.set("late-service");

    assertEquals("late-service", SdkResourceServiceName.get(5, TimeUnit.SECONDS));
  }
}
