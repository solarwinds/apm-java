package com.solarwinds.opentelemetry.extensions.config;

import static com.solarwinds.opentelemetry.extensions.SharedNames.SW_OTEL_PROXY_HOST_KEY;
import static com.solarwinds.opentelemetry.extensions.SharedNames.SW_OTEL_PROXY_PORT_KEY;

import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.export.ProxyOptions;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.function.BiFunction;

public class LogRecordExporterCustomizer
    implements BiFunction<LogRecordExporter, ConfigProperties, LogRecordExporter> {

  @Override
  public LogRecordExporter apply(
      LogRecordExporter logRecordExporter, ConfigProperties configProperties) {
    if (ProxyHelper.isProxyConfigured(configProperties)
        && logRecordExporter instanceof OtlpHttpLogRecordExporter) {
      OtlpHttpLogRecordExporterBuilder builder =
          ((OtlpHttpLogRecordExporter) (logRecordExporter)).toBuilder();

      return builder
          .setProxyOptions(
              ProxyOptions.create(
                  new InetSocketAddress(
                      Objects.requireNonNull(configProperties.getString(SW_OTEL_PROXY_HOST_KEY)),
                      Objects.requireNonNull(configProperties.getInt(SW_OTEL_PROXY_PORT_KEY)))))
          .build();
    }

    return logRecordExporter;
  }
}
