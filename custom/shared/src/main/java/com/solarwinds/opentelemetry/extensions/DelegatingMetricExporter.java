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

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.NonNull;

public class DelegatingMetricExporter implements MetricExporter {
  private final MetricExporter delegate;

  public DelegatingMetricExporter(MetricExporter delegate) {
    this.delegate = delegate;
  }

  @Override
  public CompletableResultCode export(@NonNull Collection<MetricData> metrics) {
    if (ConfigManager.getConfigOptional(ConfigProperty.AGENT_EXPORT_METRICS_ENABLED, true)) {
      return delegate.export(metrics);
    }

    return delegate.export(
        metrics.stream()
            .filter(
                metricData ->
                    MeterProvider.samplingMeterScopeName.equals(
                            metricData.getInstrumentationScopeInfo().getName())
                        || MeterProvider.requestMeterScopeName.equals(
                            metricData.getInstrumentationScopeInfo().getName()))
            .collect(Collectors.toList()));
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
