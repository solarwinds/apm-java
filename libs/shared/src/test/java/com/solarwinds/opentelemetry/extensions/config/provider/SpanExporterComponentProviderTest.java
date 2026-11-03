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

package com.solarwinds.opentelemetry.extensions.config.provider;

import static com.solarwinds.opentelemetry.extensions.config.provider.ExporterConfigs.exporterConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.Test;

class SpanExporterComponentProviderTest {
  private final SpanExporterComponentProvider tested = new SpanExporterComponentProvider();

  @Test
  void testName() {
    assertEquals("swo_otlp", tested.getName());
  }

  @Test
  void testGetType() {
    assertEquals(SpanExporter.class, tested.getType());
  }

  @Test
  void testCreateWithProxy() {
    SpanExporter exporter =
        tested.create(
            exporterConfig(
                "swo_otlp:\n"
                    + "  endpoint: https://localhost:4318/v1/traces\n"
                    + "  proxy:\n"
                    + "    host: localhost\n"
                    + "    port: 3128\n"));

    String description = exporter.toString();
    exporter.shutdown();
    assertTrue(description.contains("proxyOptions=ProxyOptions"), description);
    assertTrue(description.contains(":3128}"), description);
  }

  @Test
  void testCreateWithoutProxy() {
    SpanExporter exporter =
        tested.create(exporterConfig("swo_otlp:\n  endpoint: https://localhost:4318/v1/traces\n"));

    String description = exporter.toString();
    exporter.shutdown();
    assertTrue(description.contains("proxyOptions=null"), description);
  }
}
