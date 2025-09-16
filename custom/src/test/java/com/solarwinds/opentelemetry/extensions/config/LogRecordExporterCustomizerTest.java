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
