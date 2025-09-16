package com.solarwinds.opentelemetry.extensions.config;

import static com.solarwinds.opentelemetry.extensions.SharedNames.SW_OTEL_PROXY_HOST_KEY;
import static com.solarwinds.opentelemetry.extensions.SharedNames.SW_OTEL_PROXY_PORT_KEY;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.export.ProxyOptions;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.function.BiFunction;

public class SpanExporterCustomizer
    implements BiFunction<SpanExporter, ConfigProperties, SpanExporter> {

  @Override
  public SpanExporter apply(SpanExporter spanExporter, ConfigProperties configProperties) {
    if (ProxyHelper.isProxyConfigured(configProperties)
        && spanExporter instanceof OtlpHttpSpanExporter) {
      OtlpHttpSpanExporterBuilder builder = ((OtlpHttpSpanExporter) (spanExporter)).toBuilder();
      return builder
          .setProxy(
              ProxyOptions.create(
                  new InetSocketAddress(
                      Objects.requireNonNull(configProperties.getString(SW_OTEL_PROXY_HOST_KEY)),
                      Objects.requireNonNull(configProperties.getInt(SW_OTEL_PROXY_PORT_KEY)))))
          .build();
    }

    return spanExporter;
  }
}
