package com.solarwinds.opentelemetry.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.core.util.HostTypeDetector;
import com.solarwinds.opentelemetry.core.RootSpan;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.SemanticAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SolarwindsRootSpanProcessorTest {

  @InjectMocks private SolarwindsRootSpanProcessor tested;

  @Mock private ReadWriteSpan readWriteSpanMock;

  @Captor private ArgumentCaptor<String> stringArgumentCaptor;

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
            .setAttributes(Attributes.of(SemanticAttributes.HTTP_REQUEST_METHOD, "get"))
            .build();

    MockedStatic<RootSpan> rootSpanMock = mockStatic(RootSpan.class);
    rootSpanMock
        .when(() -> RootSpan.setRootSpan(eq(readWriteSpanMock)))
        .thenAnswer(invocation -> null);
    when(readWriteSpanMock.toSpanData()).thenReturn(testSpanData);

    MockedStatic<HostTypeDetector> hostTypeDetectorMock = mockStatic(HostTypeDetector.class);
    hostTypeDetectorMock.when(HostTypeDetector::isLambda).thenReturn(true);

    tested.onStart(Context.root(), readWriteSpanMock);
    verify(readWriteSpanMock).setAttribute(stringArgumentCaptor.capture(), anyString());

    assertEquals("sw.transaction", stringArgumentCaptor.getValue());
    rootSpanMock.close();
    hostTypeDetectorMock.close();
  }
}
