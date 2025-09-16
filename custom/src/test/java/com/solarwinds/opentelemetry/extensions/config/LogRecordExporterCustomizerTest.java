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

import static com.solarwinds.opentelemetry.extensions.SharedNames.SW_OTEL_PROXY_HOST_KEY;
import static com.solarwinds.opentelemetry.extensions.SharedNames.SW_OTEL_PROXY_PORT_KEY;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogRecordExporterCustomizerTest {

  private final LogRecordExporterCustomizer tested = new LogRecordExporterCustomizer();

  @Mock private ConfigProperties configProperties;

  @Mock private LogRecordExporter nonOtlpExporter;

  @Test
  void shouldReturnSameExporterWhenProxyNotConfigured() {
    LogRecordExporter result = tested.apply(nonOtlpExporter, configProperties);
    assertSame(nonOtlpExporter, result);
  }

  @Test
  void shouldReturnNewExporterWhenProxyConfigured() {
    when(configProperties.getString(SW_OTEL_PROXY_HOST_KEY)).thenReturn("localhost");
    when(configProperties.getInt(SW_OTEL_PROXY_PORT_KEY)).thenReturn(8080);

    OtlpHttpLogRecordExporter originalExporter = OtlpHttpLogRecordExporter.builder().build();
    LogRecordExporter result = tested.apply(originalExporter, configProperties);
    assertNotSame(originalExporter, result);
  }
}
