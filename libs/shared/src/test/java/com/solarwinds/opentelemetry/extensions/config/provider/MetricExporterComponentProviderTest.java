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
import static org.mockito.Mockito.mockConstruction;

import com.solarwinds.opentelemetry.extensions.DelegatingMetricExporter;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

class MetricExporterComponentProviderTest {
  private final MetricExporterComponentProvider tested = new MetricExporterComponentProvider();

  @Test
  void testName() {
    assertEquals("swo_otlp", tested.getName());
  }

  @Test
  void testGetType() {
    assertEquals(MetricExporter.class, tested.getType());
  }

  @Test
  void testCreateWithProxyAndMetricExportDisabled() {
    List<?> arguments =
        createAndCaptureArguments(
            "swo_otlp:\n"
                + "  endpoint: https://localhost:4318/v1/metrics\n"
                + "  metric_export_enabled: false\n"
                + "  proxy:\n"
                + "    host: localhost\n"
                + "    port: 3128\n");

    String description = arguments.get(0).toString();

    assertTrue(description.contains("proxyOptions=ProxyOptions"), description);
    assertTrue(description.contains(":3128}"), description);
    assertEquals(false, arguments.get(1));
  }

  @Test
  void testCreateWithDefaults() {
    List<?> arguments =
        createAndCaptureArguments("swo_otlp:\n  endpoint: https://localhost:4318/v1/metrics\n");

    String description = arguments.get(0).toString();
    assertTrue(description.contains("proxyOptions=null"), description);
    assertEquals(true, arguments.get(1));
  }

  private List<?> createAndCaptureArguments(String yaml) {
    DeclarativeConfigProperties config = exporterConfig(yaml);
    List<List<?>> arguments = new ArrayList<>();

    try (MockedConstruction<DelegatingMetricExporter> construction =
        mockConstruction(
            DelegatingMetricExporter.class,
            (mock, context) -> arguments.add(context.arguments()))) {
      tested.create(config);

      assertEquals(1, construction.constructed().size());
    }

    MetricExporter delegate = (MetricExporter) arguments.get(0).get(0);
    delegate.shutdown();
    return arguments.get(0);
  }
}
