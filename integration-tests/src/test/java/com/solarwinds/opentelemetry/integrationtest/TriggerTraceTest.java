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
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class TriggerTraceTest {

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Test
  void verifyTriggerTraceStampsAttributesOnRootSpan() {
    Map<String, String> headers = new HashMap<>();
    headers.put("X-Trace-Options", "trigger-trace;custom-info=chubi;sw-keys=lo:se,check-id:123");

    Context extractedContext =
        GlobalOpenTelemetry.getPropagators()
            .getTextMapPropagator()
            .extract(Context.root(), headers, MapTextMapGetter.INSTANCE);

    Tracer tracer = GlobalOpenTelemetry.get().getTracer("test");
    Span span =
        tracer
            .spanBuilder("server-request")
            .setParent(extractedContext)
            .setSpanKind(SpanKind.SERVER)
            .startSpan();

    span.end();
    List<List<SpanData>> traces = testing.waitForTraces(1);

    assertThat(traces).hasSize(1);
    SpanData rootSpan = traces.get(0).get(0);
    assertThat(rootSpan.getAttributes().get(AttributeKey.booleanKey("TriggeredTrace"))).isTrue();

    assertThat(rootSpan.getAttributes().get(AttributeKey.stringKey("SWKeys")))
        .isEqualTo("lo:se,check-id:123");
    assertThat(rootSpan.getAttributes().get(AttributeKey.stringKey("custom-info")))
        .isEqualTo("chubi");
  }

  @Test
  void verifyTriggerTraceWithoutCustomKeysOnlyStampsTriggeredTrace() {
    Map<String, String> headers = new HashMap<>();
    headers.put("X-Trace-Options", "trigger-trace");

    Context extractedContext =
        GlobalOpenTelemetry.getPropagators()
            .getTextMapPropagator()
            .extract(Context.root(), headers, MapTextMapGetter.INSTANCE);

    Tracer tracer = GlobalOpenTelemetry.get().getTracer("test");
    Span span =
        tracer
            .spanBuilder("simple-trigger")
            .setParent(extractedContext)
            .setSpanKind(SpanKind.SERVER)
            .startSpan();
    span.end();

    List<List<SpanData>> traces = testing.waitForTraces(1);
    assertThat(traces).hasSize(1);

    SpanData rootSpan = traces.get(0).get(0);
    assertThat(rootSpan.getAttributes().get(AttributeKey.booleanKey("TriggeredTrace"))).isTrue();
    assertThat(rootSpan.getAttributes().get(AttributeKey.stringKey("SWKeys"))).isNull();
  }

  private enum MapTextMapGetter implements TextMapGetter<Map<String, String>> {
    INSTANCE;

    @Override
    public Iterable<String> keys(Map<String, String> carrier) {
      return carrier.keySet();
    }

    @Override
    public String get(Map<String, String> carrier, String key) {
      return carrier == null ? null : carrier.get(key);
    }
  }
}
