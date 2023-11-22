package com.appoptics.opentelemetry.extensions.lambda;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.appoptics.opentelemetry.extensions.TransactionNameManager;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.*;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.SemanticAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InboundMeasurementMetricsGeneratorTest {

  @InjectMocks private InboundMeasurementMetricsGenerator tested;

  @Mock private LongCounter requestCounter;

  @Mock private LongHistogram responseTime;

  @Mock private LongCounter requestErrorCounter;

  @Mock private ReadableSpan readableSpanMock;

  @Captor private ArgumentCaptor<Long> longArgumentCaptor;

  @Captor private ArgumentCaptor<Attributes> attributesArgumentCaptor;

  @Test
  void returnFalseForIsStartRequired() {
    assertFalse(tested.isStartRequired());
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
            .setAttributes(Attributes.of(SemanticAttributes.HTTP_REQUEST_METHOD, "get"))
            .build();

    when(readableSpanMock.toSpanData()).thenReturn(testSpanData);
    tested.onEnd(readableSpanMock);

    verify(requestCounter).add(anyLong(), attributesArgumentCaptor.capture());
    verify(responseTime).record(anyLong(), attributesArgumentCaptor.capture());

    boolean allMatch =
        attributesArgumentCaptor.getAllValues().stream()
            .allMatch(
                attributes ->
                    "get".equals(attributes.get(AttributeKey.stringKey("http.method")))
                        && "false".equals(attributes.get(AttributeKey.stringKey("sw.is_error")))
                        && TransactionNameManager.getTransactionName(testSpanData)
                            .equals(attributes.get(AttributeKey.stringKey("sw.transaction"))));

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
            .setAttributes(Attributes.of(SemanticAttributes.HTTP_REQUEST_METHOD, "get"))
            .build();

    when(readableSpanMock.toSpanData()).thenReturn(testSpanData);
    tested.onEnd(readableSpanMock);

    verify(requestCounter).add(anyLong(), attributesArgumentCaptor.capture());
    verify(requestErrorCounter).add(anyLong(), attributesArgumentCaptor.capture());
    verify(responseTime).record(anyLong(), attributesArgumentCaptor.capture());

    boolean allMatch =
        attributesArgumentCaptor.getAllValues().stream()
            .allMatch(
                attributes ->
                    "get".equals(attributes.get(AttributeKey.stringKey("http.method")))
                        && "true".equals(attributes.get(AttributeKey.stringKey("sw.is_error")))
                        && TransactionNameManager.getTransactionName(testSpanData)
                            .equals(attributes.get(AttributeKey.stringKey("sw.transaction"))));

    assertTrue(allMatch);
  }

  @Test
  void ensureCountersAreOnlyIncrementedByOne() {
    TestSpanData testSpanData =
        TestSpanData.builder()
            .setName("test")
            .setKind(SpanKind.SERVER)
            .setStartEpochNanos(0)
            .setEndEpochNanos(10000)
            .setHasEnded(true)
            .setStatus(StatusData.error())
            .setAttributes(Attributes.of(SemanticAttributes.HTTP_REQUEST_METHOD, "get"))
            .build();

    when(readableSpanMock.toSpanData()).thenReturn(testSpanData);
    tested.onEnd(readableSpanMock);

    verify(requestCounter).add(longArgumentCaptor.capture(), any());
    verify(requestErrorCounter).add(longArgumentCaptor.capture(), any());

    boolean allMatch = longArgumentCaptor.getAllValues().stream().allMatch(value -> value == 1);
    assertTrue(allMatch);
  }
}
