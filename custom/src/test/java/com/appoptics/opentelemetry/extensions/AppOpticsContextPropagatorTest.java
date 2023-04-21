package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.extensions.stubs.TextMapGetterStub;
import com.tracelytics.joboe.XTraceOptions;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.appoptics.opentelemetry.extensions.TriggerTraceContextKey.XTRACE_OPTIONS;
import static com.appoptics.opentelemetry.extensions.TriggerTraceContextKey.XTRACE_OPTIONS_SIGNATURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppOpticsContextPropagatorTest {

    @InjectMocks
    private AppOpticsContextPropagator appOpticsContextPropagator;

    @Mock
    private TextMapSetter<Map<?, ?>> textMapSetterMock;


    private final TextMapGetter<Map<String, String>> textMapGetterStub = new TextMapGetterStub();

    @Mock
    private Span spanMock;

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    private final String traceId = "80ad68e98d3449dfc54098b38fc466ec";
    private final String spanId = "a2d8376f3cab2837";

    private final SpanContext spanContext =
            SpanContext.create(traceId, spanId, TraceFlags.getDefault(), TraceState.builder()
                    .put("test-key", "test-value")
                    .build());

    @Test
    void verifyThatInjectReturnsSuccessfullyWhenCarrierIsNull() {
        appOpticsContextPropagator.inject(Context.current(), null, textMapSetterMock);
        verify(textMapSetterMock, never()).set(any(), anyString(), anyString());
    }

    @Test
    void verifyThatInjectReturnsSuccessfullyWhenSpanIsInvalid() {
        appOpticsContextPropagator.inject(Context.current(), new HashMap<>(), textMapSetterMock);
        verify(textMapSetterMock, never()).set(any(), anyString(), anyString());
    }

    @Test
    void verifyThatTracestateIsUpdatedAndInjected() {
        try (MockedStatic<Span> spanMockedStatic = mockStatic(Span.class)) {
            spanMockedStatic.when(() -> Span.fromContext(any())).thenReturn(spanMock);
            when(spanMock.getSpanContext()).thenReturn(spanContext);

            appOpticsContextPropagator.inject(Context.current(), new HashMap<>(), textMapSetterMock);
            verify(textMapSetterMock, atMostOnce()).set(any(), anyString(), stringArgumentCaptor.capture());
            assertEquals(String.format("sw=%s-00,test-key=test-value", "a2d8376f3cab2837"),
                    stringArgumentCaptor.getValue());
        }
    }


    @Test
    void verifyThatTracestateThatExceeds512CharactersIsTruncatedAndInjected() {
        try (MockedStatic<Span> spanMockedStatic = mockStatic(Span.class)) {
            StringBuilder builder = new StringBuilder();
            new Random()
                    .ints(512L)
                    .mapToObj(Integer::toString)
                    .forEach(builder::append);

            SpanContext spanContext =
                    SpanContext.create(traceId, spanId, TraceFlags.getDefault(), TraceState.builder()
                            .put("test-key", "test-value")
                            .put("test-key-0", builder.toString())
                            .build());

            spanMockedStatic.when(() -> Span.fromContext(any())).thenReturn(spanMock);
            when(spanMock.getSpanContext()).thenReturn(spanContext);

            appOpticsContextPropagator.inject(Context.current(), new HashMap<>(), textMapSetterMock);
            verify(textMapSetterMock, atMostOnce()).set(any(), anyString(), stringArgumentCaptor.capture());
            assertEquals(String.format("sw=%s-00,test-key=test-value", "a2d8376f3cab2837"),
                    stringArgumentCaptor.getValue());
        }
    }

    @Test
    void verifyThatXtraceOptionsIsInjected() {
        try (MockedStatic<Span> spanMockedStatic = mockStatic(Span.class)) {
            String xtraceOptions = "trigger-trace;custom-senderhost=chubi;sw-keys=lo:se,check-id:123";
            Context context = Context.current().
                    with(XTRACE_OPTIONS, xtraceOptions);

            spanMockedStatic.when(() -> Span.fromContext(context)).thenReturn(spanMock);
            when(spanMock.getSpanContext()).thenReturn(spanContext);

            appOpticsContextPropagator.inject(context, new HashMap<>(), textMapSetterMock);
            verify(textMapSetterMock, times(2)).set(any(), anyString(), stringArgumentCaptor.capture());
            assertEquals(xtraceOptions, stringArgumentCaptor.getAllValues().get(1));
        }
    }

    @Test
    void verifyThatXtraceOptionsSignatureIsInjected() {
        try (MockedStatic<Span> spanMockedStatic = mockStatic(Span.class)) {
            String xtraceOptions = "test-sig";
            Context context = Context.current().
                    with(XTRACE_OPTIONS_SIGNATURE, xtraceOptions);

            spanMockedStatic.when(() -> Span.fromContext(context)).thenReturn(spanMock);
            when(spanMock.getSpanContext()).thenReturn(spanContext);

            appOpticsContextPropagator.inject(context, new HashMap<>(), textMapSetterMock);
            verify(textMapSetterMock, times(2)).set(any(), anyString(), stringArgumentCaptor.capture());
            assertEquals(xtraceOptions, stringArgumentCaptor.getAllValues().get(1));
        }
    }

    @Test
    void verifyThatXtraceOptionsIsExtractedAndPutIntoContext() {
        final String key = "custom-senderhost";
        final String value = "chubi";
        final Map<String, String> carrier = new HashMap<>() {{
            put("X-Trace-Options", String.format("%s=%s;", key, value));
        }};

        Context newContext = appOpticsContextPropagator.extract(Context.current(), carrier, textMapGetterStub);
        XTraceOptions xTraceOptions = newContext.get(TriggerTraceContextKey.KEY);

        assertEquals(String.format("%s=%s;", key, value), newContext.get(XTRACE_OPTIONS));
        assertNotNull(xTraceOptions);
        assertEquals(1, xTraceOptions.getCustomKvs().size());

        xTraceOptions.getCustomKvs()
                .forEach((innerKey, innerValue) -> {
                    assertEquals(key, innerKey.getKey());
                    assertEquals(value, innerValue);
                });
    }


    @Test
    void verifyThatXtraceOptionsSignatureIsExtractedAndPutIntoContext() {
        final Map<String, String> carrier = new HashMap<>() {{
            put("X-Trace-Options", "trigger-trace;custom-senderhost=chubi;");
            put("X-Trace-Options-Signature", "test-sig");
        }};

        Context newContext = appOpticsContextPropagator.extract(Context.current(), carrier, textMapGetterStub);
        assertEquals("test-sig", newContext.get(XTRACE_OPTIONS_SIGNATURE));
    }


    @Test
    void verifyThatTracestateIsExtractedAndPutIntoContext() {
        final Map<String, String> carrier = new HashMap<>() {{
            put("tracestate", "trigger-trace=ok");
        }};

        Context newContext = appOpticsContextPropagator.extract(Context.current(), carrier, textMapGetterStub);
        assertEquals("trigger-trace=ok", newContext.get(TraceStateKey.KEY));
    }
}
