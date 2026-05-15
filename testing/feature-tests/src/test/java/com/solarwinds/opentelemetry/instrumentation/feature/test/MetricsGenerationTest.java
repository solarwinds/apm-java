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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MetricsGenerationTest {

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Test
  void verifyResponseTimeMetricIsReportedAfterServerSpan() {
    Tracer tracer = GlobalOpenTelemetry.get().getTracer("test");
    Span span =
        tracer.spanBuilder("GET /petclinic/api/owners").setSpanKind(SpanKind.SERVER).startSpan();
    span.end();

    testing.waitForTraces(1);
    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              List<MetricData> metrics = testing.metrics();
              assertThat(metrics)
                  .as("trace.service.response_time metric should be reported")
                  .anyMatch(m -> m.getName().equals("trace.service.response_time"));
            });
  }

  @Test
  void verifyRequestCountMetricIsReportedAfterServerSpan() {
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
                  .as("trace.service.request_count metric should be reported")
                  .anyMatch(m -> m.getName().equals("trace.service.request_count"));
            });
  }

  @Test
  void verifyTokenbucketExhaustionCountMetricIsReported() {
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
                  .as("trace.service.tokenbucket_exhaustion_count metric should be reported")
                  .anyMatch(m -> m.getName().equals("trace.service.tokenbucket_exhaustion_count"));
            });
  }

  @Test
  void verifyTraceCountMetricIsReported() {
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
                  .as("trace.service.tracecount metric should be reported")
                  .anyMatch(m -> m.getName().equals("trace.service.tracecount"));
            });
  }

  @Test
  void verifySampleCountMetricIsReported() {
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
                  .as("trace.service.samplecount metric should be reported")
                  .anyMatch(m -> m.getName().equals("trace.service.samplecount"));
            });
  }

  @Test
  void verifyThroughTraceCountMetricIsReported() {
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
                  .as("trace.service.through_trace_count metric should be reported")
                  .anyMatch(m -> m.getName().equals("trace.service.through_trace_count"));
            });
  }

  @Test
  void verifyTriggeredTraceCountMetricIsReported() {
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
                  .as("trace.service.triggered_trace_count metric should be reported")
                  .anyMatch(m -> m.getName().equals("trace.service.triggered_trace_count"));
            });
  }
}
