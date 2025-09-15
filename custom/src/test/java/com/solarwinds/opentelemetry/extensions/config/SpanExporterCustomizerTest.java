package com.solarwinds.opentelemetry.extensions.config;

import static com.solarwinds.opentelemetry.extensions.SharedNames.SW_OTEL_PROXY_HOST_KEY;
import static com.solarwinds.opentelemetry.extensions.SharedNames.SW_OTEL_PROXY_PORT_KEY;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpanExporterCustomizerTest {

  private final SpanExporterCustomizer tested = new SpanExporterCustomizer();

  @Mock private ConfigProperties configProperties;

  @Mock private SpanExporter nonOtlpExporter;

  @Test
  void shouldReturnSameExporterWhenProxyNotConfigured() {
    SpanExporter result = tested.apply(nonOtlpExporter, configProperties);
    assertSame(nonOtlpExporter, result);
  }

  @Test
  void shouldReturnNewExporterWhenProxyConfigured() {
    when(configProperties.getString(SW_OTEL_PROXY_HOST_KEY)).thenReturn("localhost");
    when(configProperties.getInt(SW_OTEL_PROXY_PORT_KEY)).thenReturn(8080);

    OtlpHttpSpanExporter originalExporter = OtlpHttpSpanExporter.builder().build();
    SpanExporter result = tested.apply(originalExporter, configProperties);
    assertNotSame(originalExporter, result);
  }
}
