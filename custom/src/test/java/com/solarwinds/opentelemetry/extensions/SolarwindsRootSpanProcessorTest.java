package com.solarwinds.opentelemetry.extensions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SolarwindsRootSpanProcessorTest {

  @InjectMocks private SolarwindsRootSpanProcessor tested;

  @Mock private ReadWriteSpan readWriteSpanMock;

  @Test
  void verifySetIsCalledOnRootSpan() {
    MockedStatic<RootSpan> rootSpanMock = mockStatic(RootSpan.class);
    rootSpanMock
        .when(() -> RootSpan.setRootSpan(eq(readWriteSpanMock)))
        .thenAnswer(invocation -> null);

    tested.onStart(Context.root(), readWriteSpanMock);
    rootSpanMock.verify(() -> RootSpan.setRootSpan(any()));
    rootSpanMock.close();
  }

  @Test
  void verifyClearIsCalledOnRootSpan() {
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
    rootSpanMock.when(() -> RootSpan.clearRootSpan(anyString())).thenAnswer(invocation -> null);

    when(readWriteSpanMock.toSpanData()).thenReturn(testSpanData);
    when(readWriteSpanMock.getSpanContext()).thenReturn(testSpanData.getParentSpanContext());

    tested.onEnd(readWriteSpanMock);
    rootSpanMock.verify(() -> RootSpan.clearRootSpan(any()));
    rootSpanMock.close();
  }
}
