package com.solarwinds.opentelemetry.extensions;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.function.BiFunction;

public class MetricExporterCustomizer
    implements BiFunction<MetricExporter, ConfigProperties, MetricExporter> {

  @Override
  public MetricExporter apply(MetricExporter metricExporter, ConfigProperties configProperties) {
    return new DelegatingMetricExporter(metricExporter);
  }
}
