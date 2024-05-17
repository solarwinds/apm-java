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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PropertiesSupplier implements Supplier<Map<String, String>> {

  private final Map<String, String> defaultProperties = new HashMap<>();

  public PropertiesSupplier() {
    defaultProperties.put(
        "otel.propagators", String.format("tracecontext,baggage,%s,xray", COMPONENT_NAME));
    defaultProperties.put("otel.instrumentation.runtime-telemetry.enabled", "false");
    defaultProperties.put("otel.exporter.otlp.protocol", "grpc");
  }

  @Override
  public Map<String, String> get() {
    return defaultProperties;
  }
}
