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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.bootstrap.http.HttpServerResponseMutator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResponseHeaderCustomizerTest {

  private ResponseHeaderCustomizer customizer;

  @Mock private HttpServerResponseMutator<Object> responseMutator;

  private Object response;

  @BeforeEach
  void setUp() {
    customizer = new ResponseHeaderCustomizer();
    response = new Object();
  }

  @Test
  void shouldAddXTraceHeaderWhenSpanIsSampled() {
    String traceId = "0af7651916cd43dd8448eb211c80319c";
    String spanId = "b7ad6b7169203331";
    SpanContext spanContext =
        SpanContext.create(traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault());

    Context context = Context.root().with(Span.wrap(spanContext));
    customizer.customize(context, response, responseMutator);

    verify(responseMutator)
        .appendHeader(
            eq(response),
            eq("X-Trace"),
            eq("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01"));
  }

  @Test
  void shouldAddXTraceHeaderWhenSpanIsNotSampled() {
    String traceId = "0af7651916cd43dd8448eb211c80319c";
    String spanId = "b7ad6b7169203331";
    SpanContext spanContext =
        SpanContext.create(traceId, spanId, TraceFlags.getDefault(), TraceState.getDefault());

    Context context = Context.root().with(Span.wrap(spanContext));

    customizer.customize(context, response, responseMutator);
    verify(responseMutator)
        .appendHeader(
            eq(response),
            eq("X-Trace"),
            eq("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-00"));
  }

  @Test
  void shouldAddXTraceOptionsResponseHeaderWhenPresent() {
    String traceId = "0af7651916cd43dd8448eb211c80319c";
    String spanId = "b7ad6b7169203331";
    String xtraceOptionsResp = "trigger-trace";

    TraceState traceState =
        TraceState.builder().put("xtrace_options_response", xtraceOptionsResp).build();
    SpanContext spanContext =
        SpanContext.create(traceId, spanId, TraceFlags.getSampled(), traceState);

    Context context = Context.root().with(Span.wrap(spanContext));

    customizer.customize(context, response, responseMutator);
    verify(responseMutator).appendHeader(eq(response), eq("X-Trace"), anyString());
    verify(responseMutator)
        .appendHeader(eq(response), eq("X-Trace-Options-Response"), eq("trigger-trace"));
  }

  @Test
  void shouldNotAddXTraceOptionsResponseHeaderWhenNotPresent() {
    String traceId = "0af7651916cd43dd8448eb211c80319c";
    String spanId = "b7ad6b7169203331";
    SpanContext spanContext =
        SpanContext.create(traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault());

    Context context = Context.root().with(Span.wrap(spanContext));

    customizer.customize(context, response, responseMutator);
    verify(responseMutator).appendHeader(eq(response), eq("X-Trace"), anyString());
    verify(responseMutator, never())
        .appendHeader(eq(response), eq("X-Trace-Options-Response"), anyString());
  }

  @Test
  void shouldRecoverEncodedXTraceOptionsResponse() {
    String traceId = "0af7651916cd43dd8448eb211c80319c";
    String spanId = "b7ad6b7169203331";
    String encodedValue = "key####value....another####pair";

    TraceState traceState =
        TraceState.builder().put("xtrace_options_response", encodedValue).build();
    SpanContext spanContext =
        SpanContext.create(traceId, spanId, TraceFlags.getSampled(), traceState);

    Context context = Context.root().with(Span.wrap(spanContext));
    customizer.customize(context, response, responseMutator);
    verify(responseMutator)
        .appendHeader(eq(response), eq("X-Trace-Options-Response"), eq("key=value,another=pair"));
  }

  @Test
  void shouldHandleMultipleEncodedCharactersInXTraceOptionsResponse() {
    String traceId = "0af7651916cd43dd8448eb211c80319c";
    String spanId = "b7ad6b7169203331";
    String encodedValue = "a####b####c....d....e####f";

    TraceState traceState =
        TraceState.builder().put("xtrace_options_response", encodedValue).build();
    SpanContext spanContext =
        SpanContext.create(traceId, spanId, TraceFlags.getSampled(), traceState);

    Context context = Context.root().with(Span.wrap(spanContext));
    customizer.customize(context, response, responseMutator);
    verify(responseMutator)
        .appendHeader(eq(response), eq("X-Trace-Options-Response"), eq("a=b=c,d,e=f"));
  }
}
