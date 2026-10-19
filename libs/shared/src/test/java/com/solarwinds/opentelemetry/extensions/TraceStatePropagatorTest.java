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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solarwinds.opentelemetry.extensions.stubs.TextMapGetterStub;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TraceStatePropagatorTest {

  @InjectMocks private TraceStatePropagator traceStatePropagator;

  @Mock private TextMapSetter<Map<?, ?>> textMapSetterMock;

  private final TextMapGetter<Map<String, String>> textMapGetterStub = new TextMapGetterStub();

  @Mock private Span spanMock;

  @Captor private ArgumentCaptor<String> stringArgumentCaptor;

  private static final String traceId = "80ad68e98d3449dfc54098b38fc466ec";

  private static final String spanId = "a2d8376f3cab2837";

  private final SpanContext spanContext =
      SpanContext.create(
          traceId,
          spanId,
          TraceFlags.getDefault(),
          TraceState.builder().put("test-key", "test-value").build());

  @Test
  void verifyThatInjectReturnsSuccessfullyWhenCarrierIsNull() {
    traceStatePropagator.inject(Context.current(), null, textMapSetterMock);
    verify(textMapSetterMock, never()).set(any(), anyString(), anyString());
  }

  @Test
  void verifyThatInjectReturnsSuccessfullyWhenSpanIsInvalid() {
    traceStatePropagator.inject(Context.current(), new HashMap<>(), textMapSetterMock);
    verify(textMapSetterMock, never()).set(any(), anyString(), anyString());
  }

  @Test
  void verifyThatTracestateIsUpdatedAndInjectedMaintainingLeftmostInserts() {
    try (MockedStatic<Span> spanMockedStatic = mockStatic(Span.class)) {
      spanMockedStatic.when(() -> Span.fromContext(any())).thenReturn(spanMock);
      when(spanMock.getSpanContext()).thenReturn(spanContext);

      traceStatePropagator.inject(Context.current(), new HashMap<>(), textMapSetterMock);
      verify(textMapSetterMock, atMostOnce())
          .set(any(), anyString(), stringArgumentCaptor.capture());
      assertEquals(
          String.format("sw=%s-00,test-key=test-value", "a2d8376f3cab2837"),
          stringArgumentCaptor.getValue());
    }
  }

  @Test
  void verifyThatTracestateThatExceeds512CharactersIsTruncatedAndInjected() {
    try (MockedStatic<Span> spanMockedStatic = mockStatic(Span.class)) {
      StringBuilder builder = new StringBuilder();
      new Random().ints(512L).mapToObj(Integer::toString).forEach(builder::append);

      SpanContext spanContext =
          SpanContext.create(
              traceId,
              spanId,
              TraceFlags.getDefault(),
              TraceState.builder()
                  .put("test-key", "test-value")
                  .put("test-key-0", builder.toString())
                  .build());

      spanMockedStatic.when(() -> Span.fromContext(any())).thenReturn(spanMock);
      when(spanMock.getSpanContext()).thenReturn(spanContext);

      traceStatePropagator.inject(Context.current(), new HashMap<>(), textMapSetterMock);
      verify(textMapSetterMock, atMostOnce())
          .set(any(), anyString(), stringArgumentCaptor.capture());
      assertEquals(
          String.format("sw=%s-00,test-key=test-value", "a2d8376f3cab2837"),
          stringArgumentCaptor.getValue());
    }
  }

  @Test
  void verifyThatTracestateIsExtractedAndPutIntoContext() {
    final Map<String, String> carrier =
        new HashMap<String, String>() {
          {
            put("tracestate", "trigger-trace=ok");
          }
        };

    Context newContext =
        traceStatePropagator.extract(Context.current(), carrier, textMapGetterStub);
    assertEquals("trigger-trace=ok", newContext.get(TraceStateKey.KEY));
  }

  @Test
  void verifyThatTracestateIsNotPutIntoContextWhenNoVendorInfo() {
    final Map<String, String> carrier =
        new HashMap<String, String>() {
          {
            put("tracestate", "sw=blah");
          }
        };

    Context newContext =
        traceStatePropagator.extract(Context.current(), carrier, textMapGetterStub);
    assertNull(newContext.get(TraceStateKey.KEY));
  }
}
