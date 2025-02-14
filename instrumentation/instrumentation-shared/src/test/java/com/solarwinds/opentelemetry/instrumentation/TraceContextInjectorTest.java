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

package com.solarwinds.opentelemetry.instrumentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.IdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TraceContextInjectorTest {

  @Mock private Span spanMock;

  @Mock private SpanContext spanContextMock;

  private final IdGenerator idGenerator = IdGenerator.random();

  @Test
  void returnSqlWithTraceContextInjected() {
    String sql = "select name from students";
    String traceId = idGenerator.generateTraceId();
    String spanId = idGenerator.generateSpanId();

    try (MockedStatic<Span> spanMockedStatic = mockStatic(Span.class)) {
      spanMockedStatic.when(() -> Span.fromContext(any())).thenReturn(spanMock);
      when(spanMock.getSpanContext()).thenReturn(spanContextMock);

      when(spanContextMock.isValid()).thenReturn(true);
      when(spanContextMock.isSampled()).thenReturn(true);
      when(spanContextMock.getTraceId()).thenReturn(traceId);

      when(spanContextMock.getSpanId()).thenReturn(spanId);

      String actual = TraceContextInjector.inject(Context.current(), sql);
      String expected = String.format("/*traceparent='00-%s-%s-01'*/ %s", traceId, spanId, sql);
      assertEquals(expected, actual);
    }
  }

  @Test
  void returnSqlWithoutTraceContextWhenSpanIsNotSampled() {
    String sql = "select name from students";

    try (MockedStatic<Span> spanMockedStatic = mockStatic(Span.class)) {
      spanMockedStatic.when(() -> Span.fromContext(any())).thenReturn(spanMock);
      when(spanMock.getSpanContext()).thenReturn(spanContextMock);

      when(spanContextMock.isValid()).thenReturn(true);
      when(spanContextMock.isSampled()).thenReturn(false);

      String actual = TraceContextInjector.inject(Context.current(), sql);
      assertEquals(sql, actual);
    }
  }
}
