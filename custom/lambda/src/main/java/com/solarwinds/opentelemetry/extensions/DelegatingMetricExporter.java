package com.solarwinds.opentelemetry.extensions;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Collection;
import lombok.NonNull;

public class DelegatingMetricExporter implements MetricExporter {
  private final MetricExporter delegate;

  public DelegatingMetricExporter(MetricExporter delegate) {
    this.delegate = delegate;
  }

  @Override
  public CompletableResultCode export(@NonNull Collection<MetricData> metrics) {
    return delegate.export(metrics);
  }

  @Override
  public CompletableResultCode flush() {
    return delegate.flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }

  @Override
  public AggregationTemporality getAggregationTemporality(@NonNull InstrumentType instrumentType) {
    if (instrumentType == InstrumentType.HISTOGRAM) {
      return AggregationTemporality.DELTA;
    }
    return delegate.getAggregationTemporality(instrumentType);
  }
}
