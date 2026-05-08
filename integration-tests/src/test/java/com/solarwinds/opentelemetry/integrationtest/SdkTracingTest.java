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

package com.solarwinds.opentelemetry.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SdkTracingTest {

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Test
  void verifyManualSpanCreationWithSdkTracer() {
    testing.runWithSpan(
        "root",
        () -> {
          Tracer tracer = GlobalOpenTelemetry.get().getTracer("sdk.tracing");
          Span span = tracer.spanBuilder("greet-span").startSpan();
          span.setAttribute("sw.test.source", "SDK.trace.test");
          span.end();
        });

    List<List<SpanData>> traces = testing.waitForTraces(1);
    assertThat(traces).hasSize(1);

    List<SpanData> spans = traces.get(0);
    assertThat(spans).hasSize(2);

    SpanData greetSpan =
        spans.stream().filter(s -> "greet-span".equals(s.getName())).findFirst().orElseThrow();
    assertThat(greetSpan.getAttributes().get(AttributeKey.stringKey("sw.test.source")))
        .isEqualTo("SDK.trace.test");
  }

  @Test
  void verifyCodeStacktraceIsCaptured() {
    testing.runWithSpan(
        "root",
        () -> {
          Tracer tracer = GlobalOpenTelemetry.get().getTracer("sdk.tracing");
          Span span = tracer.spanBuilder("code-stacktrace").startSpan();
          span.setAttribute("thread.id", "test-1");
          span.end();
        });

    List<List<SpanData>> traces = testing.waitForTraces(1);
    assertThat(traces).hasSize(1);

    List<SpanData> spans = traces.get(0);
    assertThat(spans).hasSize(2);

    SpanData spanData =
        spans.stream().filter(s -> "code-stacktrace".equals(s.getName())).findFirst().orElseThrow();
    assertThat(spanData.getAttributes().get(AttributeKey.stringKey("code.stacktrace"))).isNotNull();
  }

  @Test
  void verifyResourceNameFromServiceKey() {
    testing.runWithSpan(
        "root",
        () -> {
          Tracer tracer = GlobalOpenTelemetry.get().getTracer("sdk.tracing");
          Span span = tracer.spanBuilder("code-stacktrace").startSpan();
          span.setAttribute("thread.id", "test-1");
          span.end();
        });

    List<List<SpanData>> traces = testing.waitForTraces(1);
    assertThat(traces).hasSize(1);

    List<SpanData> spans = traces.get(0);
    assertThat(spans).hasSize(2);

    SpanData spanData =
        spans.stream().filter(s -> "code-stacktrace".equals(s.getName())).findFirst().orElseThrow();
    assertThat(spanData.getResource().getAttributes().get(AttributeKey.stringKey("service.name")))
        .isEqualTo("test-app");
  }
}
