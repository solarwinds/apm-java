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

import static com.solarwinds.opentelemetry.extensions.SharedNames.SW_OTEL_PROXY_HOST_KEY;
import static com.solarwinds.opentelemetry.extensions.SharedNames.SW_OTEL_PROXY_PORT_KEY;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.common.export.ProxyOptions;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricExporterCustomizerTest {
  @InjectMocks private MetricExporterCustomizer tested;

  @Mock private MetricExporter metricExporterMock;

  @Mock private ConfigProperties configProperties;

  @Test
  void verifyThatDelegatingMetricExporterIsReturned() {
    assertInstanceOf(
        DelegatingMetricExporter.class,
        tested.apply(
            metricExporterMock, DefaultConfigProperties.createFromMap(Collections.emptyMap())));
  }

  @Test
  void shouldReturnDelegatingExporterWhenProxyNotConfigured() {
    MetricExporter result = tested.apply(metricExporterMock, configProperties);
    assertInstanceOf(DelegatingMetricExporter.class, result);
  }

  @Test
  void shouldCallSetProxyOptionsWhenProxyConfigured() {
    when(configProperties.getString(SW_OTEL_PROXY_HOST_KEY)).thenReturn("localhost");
    when(configProperties.getInt(SW_OTEL_PROXY_PORT_KEY)).thenReturn(8080);

    OtlpHttpMetricExporter originalExporter = mock(OtlpHttpMetricExporter.class);
    OtlpHttpMetricExporterBuilder builder = mock(OtlpHttpMetricExporterBuilder.class);

    when(originalExporter.toBuilder()).thenReturn(builder);
    when(builder.setProxyOptions(any(ProxyOptions.class))).thenReturn(builder);
    when(builder.build()).thenReturn(mock(OtlpHttpMetricExporter.class));

    MetricExporter result = tested.apply(originalExporter, configProperties);
    verify(builder).setProxyOptions(any(ProxyOptions.class));
    assertInstanceOf(DelegatingMetricExporter.class, result);
  }
}
