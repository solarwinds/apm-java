package com.appoptics.opentelemetry.extensions.lambda;

import static com.solarwinds.joboe.core.util.HostTypeDetector.isLambda;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.function.BiFunction;

public class MetricExporterCustomizer
    implements BiFunction<MetricExporter, ConfigProperties, MetricExporter> {

  @Override
  public MetricExporter apply(MetricExporter metricExporter, ConfigProperties configProperties) {
    if (isLambda()) {
      return new DelegatingMetricExporter(metricExporter);
    }
    return metricExporter;
  }
}
