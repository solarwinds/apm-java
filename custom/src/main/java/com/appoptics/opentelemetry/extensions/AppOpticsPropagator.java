package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.core.Util;
import com.tracelytics.instrumentation.HeaderConstants;
import com.tracelytics.joboe.Metadata;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.TraceStateBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AppOpticsPropagator implements TextMapPropagator {
    private static final String TRACE_STATE_APPOPTICS_KEY = "appoptics";
    static final String TRACE_PARENT = "traceparent";
    static final String TRACE_STATE = "tracestate";
    private static final List<String> FIELDS =
            Collections.unmodifiableList(Arrays.asList(TRACE_PARENT, TRACE_STATE, HeaderConstants.XTRACE_HEADER));
    private static final int TRACESTATE_MAX_SIZE = 512;
    private static final int TRACESTATE_MAX_MEMBERS = 32;
    private static final char TRACESTATE_KEY_VALUE_DELIMITER = '=';
    private static final char TRACESTATE_ENTRY_DELIMITER = ',';

    @Override
    public Collection<String> fields() {
        return FIELDS;
    }

    @Override
    public <C> void inject(Context context, @Nullable C carrier, TextMapSetter<C> setter) {
        if (context == null || setter == null) {
            return;
        }

        SpanContext spanContext = Span.fromContext(context).getSpanContext();
        if (!spanContext.isValid()) {
            return;
        }

        Metadata metadata = Util.buildMetadata(spanContext);

        if (metadata.isValid()) {
            String xTraceId = metadata.toHexString();
            setter.set(carrier, HeaderConstants.XTRACE_HEADER, xTraceId);

            //update trace state too: https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/api.md#tracestate
            //https://www.w3.org/TR/trace-context/#mutating-the-tracestate-field
            TraceState traceState = spanContext.getTraceState();
            StringBuilder stringBuilder = new StringBuilder(TRACESTATE_MAX_SIZE);

            stringBuilder.append(TRACE_STATE_APPOPTICS_KEY).append(TRACESTATE_KEY_VALUE_DELIMITER).append(metadata.toHexString());
            AtomicInteger count = new AtomicInteger(1);
            traceState.forEach(
                    (key, value) -> {
                        if (!TRACE_STATE_APPOPTICS_KEY.equals(key) && count.getAndIncrement() <= TRACESTATE_MAX_MEMBERS) {
                            stringBuilder.append(TRACESTATE_ENTRY_DELIMITER);
                            stringBuilder.append(key).append(TRACESTATE_KEY_VALUE_DELIMITER).append(value);
                        }
                    });
            setter.set(carrier, TRACE_STATE, stringBuilder.toString());
        }
    }

    @Override
    public <C> Context extract(Context context, @Nullable C carrier, TextMapGetter<C> getter) {
        if (context == null || getter == null) {
            return context;
        }

        //it should have a spanContext extracted by W3CTraceContextPropagator
        //since we expect propagators config: `-Dotel.propagators=tracecontext,baggage,appoptics`
        SpanContext spanContext = Span.fromContext(context).getSpanContext();
        if (!spanContext.isValid()) {
            return context;
        }

        String xTraceId = getter.get(carrier, HeaderConstants.XTRACE_HEADER);
        if (xTraceId == null) { //then try W3C tracestate
            xTraceId = spanContext.getTraceState().get(TRACE_STATE_APPOPTICS_KEY);
        }

        if (xTraceId == null) {
            return context;
        }

        TraceStateBuilder traceStateBuilder = Span.fromContext(context).getSpanContext().getTraceState().toBuilder();
        //step 1: include full x-trace ID as trace state
        traceStateBuilder.put(TRACE_STATE_APPOPTICS_KEY, xTraceId);

        Util.W3TraceContextHolder w3Context = Util.toW3TraceContext(xTraceId);
        //step 2: create span context from ao x-trace ID
        SpanContext newSpanContext = SpanContext.createFromRemoteParent(w3Context.traceId, w3Context.spanId, w3Context.traceFlags, traceStateBuilder.build());

        return context.with(Span.wrap(newSpanContext));
    }



}
