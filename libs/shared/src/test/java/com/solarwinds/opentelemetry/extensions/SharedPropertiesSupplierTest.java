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

package com.solarwinds.opentelemetry.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SharedPropertiesSupplierTest {

  private final Map<String, String> properties = new SharedPropertiesSupplier().get();

  @Test
  void disablesServiceInstanceIdResourceProvider() {
    assertEquals(
        "io.opentelemetry.sdk.autoconfigure.ServiceInstanceIdResourceProvider",
        properties.get("otel.java.disabled.resource.providers"));
  }

  @Test
  void usesSolarwindsSpanStacktraceFilter() {
    assertEquals(
        SharedNames.SPAN_STACKTRACE_FILTER_CLASS,
        properties.get("otel.java.experimental.span-stacktrace.filter"));
  }

  @Test
  void configuresDeltaTemporalityAndExponentialHistograms() {
    assertEquals("delta", properties.get("otel.exporter.otlp.metrics.temporality.preference"));
    assertEquals(
        "base2_exponential_bucket_histogram",
        properties.get("otel.exporter.otlp.metrics.default.histogram.aggregation"));
  }

  @Test
  void optsIntoDuplicateDatabaseSemconv() {
    assertEquals("database/dup", properties.get("otel.semconv-stability.opt-in"));
  }
}
