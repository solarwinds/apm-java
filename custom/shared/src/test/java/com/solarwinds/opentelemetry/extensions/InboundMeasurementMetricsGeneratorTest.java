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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.HttpAttributes;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
class InboundMeasurementMetricsGeneratorTest {

  @InjectMocks private InboundMeasurementMetricsGenerator tested;

  @Mock private LongHistogram responseTime;

  @Mock private ReadableSpan readableSpanMock;

  @Mock private ReadWriteSpan readWriteSpanMock;

  @Captor private ArgumentCaptor<String> stringArgumentCaptor;

  @Captor private ArgumentCaptor<Long> longArgumentCaptor;

  @Captor private ArgumentCaptor<Attributes> attributesArgumentCaptor;

  @Test
  void returnFalseForIsStartRequired() {
    assertTrue(tested.isStartRequired());
  }

  @Test
  void returnTrueForIsEndRequired() {
    assertTrue(tested.isEndRequired());
  }

  @Test
  void verifyDurationIsCorrect() {
    TestSpanData testSpanData =
        TestSpanData.builder()
            .setName("test")
            .setKind(SpanKind.SERVER)
            .setStartEpochNanos(0)
            .setEndEpochNanos(10_000_000)
            .setHasEnded(true)
            .setStatus(StatusData.ok())
            .build();
    when(readableSpanMock.toSpanData()).thenReturn(testSpanData);

    tested.onEnd(readableSpanMock);
    verify(responseTime).record(longArgumentCaptor.capture(), any());
    assertEquals(10, longArgumentCaptor.getValue());
  }

  @Test
  void verifyMetricAttributeValuesIsCorrectWhenNotErrorSpan() {
    TestSpanData testSpanData =
        TestSpanData.builder()
            .setName("test")
            .setKind(SpanKind.SERVER)
            .setStartEpochNanos(0)
            .setEndEpochNanos(10000)
            .setHasEnded(true)
            .setStatus(StatusData.ok())
            .setAttributes(
                Attributes.of(
                    HttpAttributes.HTTP_REQUEST_METHOD,
                    "get",
                    HttpAttributes.HTTP_RESPONSE_STATUS_CODE,
                    200L))
            .build();

    when(readableSpanMock.toSpanData()).thenReturn(testSpanData);
    when(readableSpanMock.getKind()).thenReturn(testSpanData.getKind());
    tested.onEnd(readableSpanMock);

    verify(responseTime).record(anyLong(), attributesArgumentCaptor.capture());

    boolean allMatch =
        attributesArgumentCaptor.getAllValues().stream()
            .allMatch(
                attributes ->
                    "get".equals(attributes.get(AttributeKey.stringKey("http.method")))
                        && Objects.equals(
                            200L, attributes.get(AttributeKey.longKey("http.status_code")))
                        && !attributes.get(AttributeKey.booleanKey("sw.is_error"))
                        && TransactionNameManager.getTransactionName(testSpanData)
                            .equals(
                                attributes.get(
                                    AttributeKey.stringKey(SharedNames.TRANSACTION_NAME_KEY))));

    assertTrue(allMatch);
  }

  @Test
  void verifyMetricAttributeValuesIsCorrectWhenErrorSpan() {
    TestSpanData testSpanData =
        TestSpanData.builder()
            .setName("test")
            .setKind(SpanKind.SERVER)
            .setStartEpochNanos(0)
            .setEndEpochNanos(10000)
            .setHasEnded(true)
            .setStatus(StatusData.error())
            .setAttributes(
                Attributes.of(
                    HttpAttributes.HTTP_REQUEST_METHOD,
                    "get",
                    HttpAttributes.HTTP_RESPONSE_STATUS_CODE,
                    500L))
            .build();

    when(readableSpanMock.toSpanData()).thenReturn(testSpanData);
    when(readableSpanMock.getKind()).thenReturn(testSpanData.getKind());
    tested.onEnd(readableSpanMock);

    verify(responseTime).record(anyLong(), attributesArgumentCaptor.capture());

    boolean allMatch =
        attributesArgumentCaptor.getAllValues().stream()
            .allMatch(
                attributes ->
                    "get".equals(attributes.get(AttributeKey.stringKey("http.method")))
                        && Objects.equals(
                            500L, attributes.get(AttributeKey.longKey("http.status_code")))
                        && attributes.get(AttributeKey.booleanKey("sw.is_error"))
                        && TransactionNameManager.getTransactionName(testSpanData)
                            .equals(
                                attributes.get(
                                    AttributeKey.stringKey(SharedNames.TRANSACTION_NAME_KEY))));

    assertTrue(allMatch);
  }

  @Test
  void verifyHttpAttributeIsNotAddedWhenNotServerSpan() {
    TestSpanData testSpanData =
        TestSpanData.builder()
            .setName("test")
            .setKind(SpanKind.CLIENT)
            .setStartEpochNanos(0)
            .setEndEpochNanos(10000)
            .setHasEnded(true)
            .setStatus(StatusData.error())
            .setAttributes(
                Attributes.of(
                    HttpAttributes.HTTP_REQUEST_METHOD,
                    "get",
                    HttpAttributes.HTTP_RESPONSE_STATUS_CODE,
                    500L))
            .build();

    when(readableSpanMock.toSpanData()).thenReturn(testSpanData);
    tested.onEnd(readableSpanMock);

    verify(responseTime).record(anyLong(), attributesArgumentCaptor.capture());

    boolean allMatch =
        attributesArgumentCaptor.getAllValues().stream()
            .allMatch(
                attributes ->
                    attributes.get(AttributeKey.stringKey("http.method")) == null
                        && attributes.get(AttributeKey.longKey("http.status_code")) == null);

    assertTrue(allMatch);
  }

  @Test
  void verifyTransactionNameIsSetOnRootSpan() {
    TestSpanData testSpanData =
        TestSpanData.builder()
            .setName("test")
            .setKind(SpanKind.SERVER)
            .setStartEpochNanos(0)
            .setEndEpochNanos(10000)
            .setHasEnded(true)
            .setStatus(StatusData.ok())
            .setAttributes(Attributes.of(HttpAttributes.HTTP_REQUEST_METHOD, "get"))
            .build();

    when(readWriteSpanMock.toSpanData()).thenReturn(testSpanData);
    tested.onStart(Context.root(), readWriteSpanMock);

    verify(readWriteSpanMock).setAttribute(stringArgumentCaptor.capture(), anyString());

    assertEquals("sw.transaction", stringArgumentCaptor.getValue());
  }
}
