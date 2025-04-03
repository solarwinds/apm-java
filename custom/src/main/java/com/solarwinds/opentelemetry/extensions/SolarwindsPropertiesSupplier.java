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

import static com.solarwinds.opentelemetry.extensions.SharedNames.COMPONENT_NAME;
import static com.solarwinds.opentelemetry.extensions.SharedNames.SPAN_STACKTRACE_FILTER_CLASS;
import static com.solarwinds.opentelemetry.extensions.initialize.AutoConfigurationCustomizerProviderImpl.isAgentEnabled;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Provide various default properties when running OT agent with AO SPI implementations */
public class SolarwindsPropertiesSupplier implements Supplier<Map<String, String>> {
  private static final Map<String, String> PROPERTIES = new HashMap<>();

  static {
    if (isAgentEnabled()) {
      PROPERTIES.put("otel.metrics.exporter", "none");
      PROPERTIES.put("otel.logs.exporter", "none");
      PROPERTIES.put("otel.exporter.otlp.compression", "gzip");

      PROPERTIES.put("otel.java.experimental.span-stacktrace.filter", SPAN_STACKTRACE_FILTER_CLASS);
      PROPERTIES.put("otel.propagators", String.format("tracecontext,baggage,%s", COMPONENT_NAME));
      PROPERTIES.put("otel.semconv-stability.opt-in", "database/dup");
    } else {
      PROPERTIES.put("otel.sdk.disabled", "true");
    }
  }

  @Override
  public Map<String, String> get() {
    return PROPERTIES;
  }
}
