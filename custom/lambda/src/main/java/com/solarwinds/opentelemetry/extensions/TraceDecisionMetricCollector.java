/*
 * Copyright SolarWinds Worldwide, LLC.
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

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.sampling.TraceDecisionUtil;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.LinkedList;
import java.util.List;

@AutoService(AgentListener.class)
public class TraceDecisionMetricCollector implements AutoCloseable, AgentListener {
  private final List<ObservableLongGauge> gauges = new LinkedList<>();

  public void collect(Meter meter) {
    gauges.add(
        meter
            .gaugeBuilder("trace.service.request_count")
            .ofLongs()
            .buildWithCallback(
                observableLongMeasurement ->
                    observableLongMeasurement.record(
                        TraceDecisionUtil.consumeMetricsData(
                            TraceDecisionUtil.MetricType.THROUGHPUT))));

    gauges.add(
        meter
            .gaugeBuilder("trace.service.tokenbucket_exhaustion_count")
            .ofLongs()
            .buildWithCallback(
                observableLongMeasurement ->
                    observableLongMeasurement.record(
                        TraceDecisionUtil.consumeMetricsData(
                            TraceDecisionUtil.MetricType.TOKEN_BUCKET_EXHAUSTION))));

    gauges.add(
        meter
            .gaugeBuilder("trace.service.tracecount")
            .ofLongs()
            .buildWithCallback(
                observableLongMeasurement ->
                    observableLongMeasurement.record(
                        TraceDecisionUtil.consumeMetricsData(
                            TraceDecisionUtil.MetricType.TRACE_COUNT))));

    gauges.add(
        meter
            .gaugeBuilder("trace.service.samplecount")
            .ofLongs()
            .buildWithCallback(
                observableLongMeasurement ->
                    observableLongMeasurement.record(
                        TraceDecisionUtil.consumeMetricsData(
                            TraceDecisionUtil.MetricType.SAMPLE_COUNT))));

    gauges.add(
        meter
            .gaugeBuilder("trace.service.through_trace_count")
            .ofLongs()
            .buildWithCallback(
                observableLongMeasurement ->
                    observableLongMeasurement.record(
                        TraceDecisionUtil.consumeMetricsData(
                            TraceDecisionUtil.MetricType.THROUGH_TRACE_COUNT))));

    gauges.add(
        meter
            .gaugeBuilder("trace.service.triggered_trace_count")
            .ofLongs()
            .buildWithCallback(
                observableLongMeasurement ->
                    observableLongMeasurement.record(
                        TraceDecisionUtil.consumeMetricsData(
                            TraceDecisionUtil.MetricType.TRIGGERED_TRACE_COUNT))));
  }

  @Override
  public void close() {
    gauges.forEach(ObservableLongGauge::close);
  }

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    collect(MeterProvider.getSamplingMetricsMeter());
  }
}
