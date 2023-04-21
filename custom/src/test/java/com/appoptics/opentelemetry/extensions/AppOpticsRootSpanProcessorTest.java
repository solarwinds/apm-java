package com.appoptics.opentelemetry.extensions;

import com.tracelytics.joboe.XTraceOptions;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppOpticsRootSpanProcessorTest {

  @InjectMocks
  private AppOpticsRootSpanProcessor appOpticsRootSpanProcessor;

  @Mock
  private ReadWriteSpan readWriteSpanMock;

  @Mock
  private SpanContext spanContextMock;

  @Captor
  private ArgumentCaptor<String> stringArgumentCaptor;

  private final TraceState traceState = TraceState.builder()
      .put("sw", "789b5fa910da28f9-01")
      .build();

  private final String traceId = "6ddb2613c236c123158100b91879c76b";


  @Test
  void verifThatTriggeredTraceAttributeIsAddedForTriggerTrace() {
    Context context = Context.current()
        .with(TriggerTraceContextKey.KEY, XTraceOptions.getXTraceOptions("trigger-trace", null));

    when(readWriteSpanMock.getSpanContext()).thenReturn(spanContextMock);
    when(spanContextMock.getTraceId()).thenReturn(traceId);

    when(spanContextMock.getTraceState()).thenReturn(TraceState.getDefault());
    appOpticsRootSpanProcessor.onStart(context, readWriteSpanMock);
    verify(readWriteSpanMock, atMostOnce()).setAttribute(stringArgumentCaptor.capture(),
        anyBoolean());

    List<String> allValues = stringArgumentCaptor.getAllValues();
    assertEquals(1, allValues.size());
    assertEquals("TriggeredTrace", allValues.get(0));
  }


  @Test
  void verifThatTriggeredTraceAttributeIsNotAddedForContinuedTrace() {
    Context context = Context.current()
        .with(TriggerTraceContextKey.KEY, XTraceOptions.getXTraceOptions("trigger-trace", null));

    when(readWriteSpanMock.getSpanContext()).thenReturn(spanContextMock);
    when(spanContextMock.getTraceId()).thenReturn(traceId);


    when(spanContextMock.getTraceState()).thenReturn(traceState);
    appOpticsRootSpanProcessor.onStart(context, readWriteSpanMock);
    verify(readWriteSpanMock, never()).setAttribute(anyString(), any());
  }

  @Test
  void verifThatCustomKvAttributesAreAdded() {
    Context context = Context.current()
        .with(TriggerTraceContextKey.KEY, XTraceOptions.getXTraceOptions("custom-chubi=chubby;", null));

    when(readWriteSpanMock.getSpanContext()).thenReturn(spanContextMock);
    when(spanContextMock.getTraceId()).thenReturn(traceId);

    appOpticsRootSpanProcessor.onStart(context, readWriteSpanMock);
    verify(readWriteSpanMock, atMostOnce()).setAttribute(stringArgumentCaptor.capture(),
        stringArgumentCaptor.capture());

    List<String> allValues = stringArgumentCaptor.getAllValues();
    assertEquals(2, allValues.size());
    assertEquals("custom-chubi=chubby", String.format("%s=%s", allValues.get(0), allValues.get(1)));
  }

  @Test
  void verifThatSwKeysAttributeIsAdded() {
    Context context = Context.current()
        .with(TriggerTraceContextKey.KEY, XTraceOptions.getXTraceOptions("sw-keys=lo:se,check-id:123", null));

    when(readWriteSpanMock.getSpanContext()).thenReturn(spanContextMock);
    when(spanContextMock.getTraceId()).thenReturn(traceId);

    appOpticsRootSpanProcessor.onStart(context, readWriteSpanMock);
    verify(readWriteSpanMock, atMostOnce()).setAttribute(stringArgumentCaptor.capture(),
        stringArgumentCaptor.capture());

    List<String> allValues = stringArgumentCaptor.getAllValues();
    assertEquals(2, allValues.size());
    assertEquals("SWKeys=lo:se,check-id:123", String.format("%s=%s", allValues.get(0), allValues.get(1)));
  }
}