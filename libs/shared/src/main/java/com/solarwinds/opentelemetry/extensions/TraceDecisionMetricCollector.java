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

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.sampling.TraceDecisionUtil;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.ArrayList;
import java.util.List;

@AutoService(AgentListener.class)
public class TraceDecisionMetricCollector implements AutoCloseable, AgentListener {
  private final List<ObservableLongGauge> gauges = new ArrayList<>();

  public void collect(Meter meter) {
    String reqCountUnit = "{request}";
    String traceCountUnit = "{trace}";
    gauges.add(
        meter
            .gaugeBuilder("trace.service.request_count")
            .setDescription("Count of all requests.")
            .setUnit(reqCountUnit)
            .ofLongs()
            .buildWithCallback(
                observableLongMeasurement ->
                    observableLongMeasurement.record(
                        TraceDecisionUtil.consumeMetricsData(
                            TraceDecisionUtil.MetricType.THROUGHPUT))));

    gauges.add(
        meter
            .gaugeBuilder("trace.service.tokenbucket_exhaustion_count")
            .setDescription(
                "Count of requests that were not traced due to token bucket rate limiting.")
            .setUnit(reqCountUnit)
            .ofLongs()
            .buildWithCallback(
                observableLongMeasurement ->
                    observableLongMeasurement.record(
                        TraceDecisionUtil.consumeMetricsData(
                            TraceDecisionUtil.MetricType.TOKEN_BUCKET_EXHAUSTION))));

    gauges.add(
        meter
            .gaugeBuilder("trace.service.tracecount")
            .setDescription("Count of all traces.")
            .setUnit(traceCountUnit)
            .ofLongs()
            .buildWithCallback(
                observableLongMeasurement ->
                    observableLongMeasurement.record(
                        TraceDecisionUtil.consumeMetricsData(
                            TraceDecisionUtil.MetricType.TRACE_COUNT))));

    gauges.add(
        meter
            .gaugeBuilder("trace.service.samplecount")
            .setDescription(
                "Count of requests that went through sampling, which excludes those with a valid upstream decision or trigger traced.")
            .setUnit(reqCountUnit)
            .ofLongs()
            .buildWithCallback(
                observableLongMeasurement ->
                    observableLongMeasurement.record(
                        TraceDecisionUtil.consumeMetricsData(
                            TraceDecisionUtil.MetricType.SAMPLE_COUNT))));

    gauges.add(
        meter
            .gaugeBuilder("trace.service.through_trace_count")
            .setDescription(
                "Count of requests with a valid upstream decision, thus passed through sampling.")
            .setUnit(reqCountUnit)
            .ofLongs()
            .buildWithCallback(
                observableLongMeasurement ->
                    observableLongMeasurement.record(
                        TraceDecisionUtil.consumeMetricsData(
                            TraceDecisionUtil.MetricType.THROUGH_TRACE_COUNT))));

    gauges.add(
        meter
            .gaugeBuilder("trace.service.triggered_trace_count")
            .setDescription("Count of triggered traces.")
            .setUnit(traceCountUnit)
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
    Runtime.getRuntime().addShutdownHook(new Thread(this::close));
  }
}
