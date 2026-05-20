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

package com.solarwinds.opentelemetry.instrumentation.feature.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.sdk.metrics.data.MetricData;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MetricsGenerationTest {

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  static Stream<Arguments> metricNames() {
    return Stream.of(
        Arguments.of("trace.service.response_time"),
        Arguments.of("trace.service.request_count"),
        Arguments.of("trace.service.tokenbucket_exhaustion_count"),
        Arguments.of("trace.service.tracecount"),
        Arguments.of("trace.service.samplecount"),
        Arguments.of("trace.service.through_trace_count"),
        Arguments.of("trace.service.triggered_trace_count"));
  }

  @ParameterizedTest(name = "{0} metric is reported")
  @MethodSource("metricNames")
  void verifyMetricIsReportedAfterServerSpan(String metricName) {
    Tracer tracer = GlobalOpenTelemetry.get().getTracer("test");
    Span span =
        tracer.spanBuilder("GET /petclinic/api/pettypes").setSpanKind(SpanKind.SERVER).startSpan();
    span.end();

    testing.waitForTraces(1);
    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              List<MetricData> metrics = testing.metrics();
              assertThat(metrics)
                  .as("%s metric should be reported", metricName)
                  .anyMatch(m -> m.getName().equals(metricName));
            });
  }
}
