package com.solarwinds.opentelemetry.extensions.config.provider;

import static io.opentelemetry.exporter.otlp.internal.OtlpConfigUtil.DATA_TYPE_METRICS;

import com.google.auto.service.AutoService;
import com.solarwinds.opentelemetry.extensions.DelegatingMetricExporter;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.exporter.otlp.internal.OtlpDeclarativeConfigUtil;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Locale;
import java.util.function.Consumer;

@AutoService(ComponentProvider.class)
public class MetricExporterComponentProvider implements ComponentProvider<MetricExporter> {
  @Override
  public Class<MetricExporter> getType() {
    return MetricExporter.class;
  }

  @Override
  public String getName() {
    return "swo/metricExporter";
  }

  @Override
  public MetricExporter create(DeclarativeConfigProperties config) {
    OtlpGrpcMetricExporterBuilder builder = OtlpGrpcMetricExporter.builder();

    OtlpDeclarativeConfigUtil.configureOtlpExporterBuilder(
        DATA_TYPE_METRICS,
        config,
        builder::setEndpoint,
        builder::addHeader,
        builder::setCompression,
        builder::setTimeout,
        builder::setTrustedCertificates,
        builder::setClientTls,
        builder::setRetryPolicy,
        builder::setMemoryMode,
        false);

    configureOtlpAggregationTemporality(config, builder::setAggregationTemporalitySelector);

    return new DelegatingMetricExporter(builder.build());
  }

  public static void configureOtlpAggregationTemporality(
      DeclarativeConfigProperties config,
      Consumer<AggregationTemporalitySelector> aggregationTemporalitySelectorConsumer) {
    String temporalityStr = config.getString("temporality_preference");
    if (temporalityStr == null) {
      return;
    }
    AggregationTemporalitySelector temporalitySelector;
    switch (temporalityStr.toLowerCase(Locale.ROOT)) {
      case "cumulative":
        temporalitySelector = AggregationTemporalitySelector.alwaysCumulative();
        break;
      case "delta":
        temporalitySelector = AggregationTemporalitySelector.deltaPreferred();
        break;
      case "lowmemory":
        temporalitySelector = AggregationTemporalitySelector.lowMemory();
        break;
      default:
        throw new ConfigurationException("Unrecognized temporality_preference: " + temporalityStr);
    }
    aggregationTemporalitySelectorConsumer.accept(temporalitySelector);
  }
}
