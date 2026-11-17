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

import static com.solarwinds.opentelemetry.extensions.SharedNames.SPAN_STACKTRACE_FILTER_CLASS;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SharedPropertiesSupplier implements Supplier<Map<String, String>> {
  private static final Map<String, String> PROPERTIES;

  static {
    PROPERTIES = new HashMap<>();
    PROPERTIES.put("otel.java.experimental.span-stacktrace.filter", SPAN_STACKTRACE_FILTER_CLASS);
    PROPERTIES.put("otel.semconv-stability.opt-in", "database/dup");

    PROPERTIES.put("otel.exporter.otlp.metrics.temporality.preference", "delta");
    PROPERTIES.put(
        "otel.exporter.otlp.metrics.default.histogram.aggregation",
        "base2_exponential_bucket_histogram");

    // TODO(revert): temporarily re-enabled to verify the smoke test catches entity inference
    // breakage.
    // PROPERTIES.put(
    //     "otel.java.disabled.resource.providers",
    //     "io.opentelemetry.sdk.autoconfigure.ServiceInstanceIdResourceProvider");
  }

  @Override
  public Map<String, String> get() {
    return PROPERTIES;
  }
}
