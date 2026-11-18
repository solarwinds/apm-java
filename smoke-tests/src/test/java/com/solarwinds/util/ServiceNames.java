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

package com.solarwinds.util;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Per-run service names. A fresh name forces the backend to infer a new service entity on every
 * run, so stale entities and telemetry from previous runs cannot satisfy the k6 checks.
 */
public final class ServiceNames {
  static final DateTimeFormatter SUFFIX_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  public static final String SMOKE_TEST = smokeTest(Clock.systemUTC());

  private ServiceNames() {}

  static String smokeTest(Clock clock) {
    return "java-apm-smoke-test-"
        + ZonedDateTime.now(clock).withZoneSameInstant(ZoneOffset.UTC).format(SUFFIX_FORMAT);
  }
}
