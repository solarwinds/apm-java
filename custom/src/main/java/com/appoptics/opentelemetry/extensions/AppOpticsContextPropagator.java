package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.core.Util;
import com.tracelytics.instrumentation.HeaderConstants;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.XTraceHeader;
import com.tracelytics.joboe.XTraceOptions;
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

public class AppOpticsContextPropagator implements TextMapPropagator {
    private static final String TRACE_STATE_APPOPTICS_KEY = "sw";
    static final String TRACE_PARENT = "traceparent";
    static final String TRACE_STATE = "tracestate";
    static final String XTRACE_OPTIONS = "xtrace-options";
    static final String XTRACE_OPTIONS_SIGNATURE = "xtrace-options-signature";
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

    /**
     * Injects the both the AppOptics x-trace ID and the updated w3c `tracestate` with our x-trace ID prepended
     * into the carrier with values provided by current context
     * @param context
     * @param carrier
     * @param setter
     * @param <C>
     */
    @Override
    public <C> void inject(Context context, @Nullable C carrier, TextMapSetter<C> setter) {
        SpanContext spanContext = Span.fromContext(context).getSpanContext();
        if (carrier == null || !spanContext.isValid()) {
            return;
        }
        //update trace state too: https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/api.md#tracestate
        //https://www.w3.org/TR/trace-context/#mutating-the-tracestate-field
        TraceState traceState = spanContext.getTraceState();
        StringBuilder traceStateBuilder = new StringBuilder(TRACESTATE_MAX_SIZE);
        String swTraceStateValue = spanContext.getSpanId() + "-" + (spanContext.isSampled() ? "01" : "00");
        traceStateBuilder.append(TRACE_STATE_APPOPTICS_KEY).append(TRACESTATE_KEY_VALUE_DELIMITER).append(swTraceStateValue);
        AtomicInteger count = new AtomicInteger(1);
        traceState.forEach(
                (key, value) -> {
                    if (!TRACE_STATE_APPOPTICS_KEY.equals(key) && count.getAndIncrement() <= TRACESTATE_MAX_MEMBERS) {
                        traceStateBuilder.append(TRACESTATE_ENTRY_DELIMITER);
                        traceStateBuilder.append(key).append(TRACESTATE_KEY_VALUE_DELIMITER).append(value);
                    }
                });
        setter.set(carrier, TRACE_STATE, traceStateBuilder.toString());

        String traceOptions = context.get(TriggerTraceContextKey.XTRACE_OPTIONS);
        String traceOptionsSignature = context.get(TriggerTraceContextKey.XTRACE_OPTIONS_SIGNATURE);
        if (traceOptions != null) {
            setter.set(carrier, XTRACE_OPTIONS, traceOptions);
        }
        if (traceOptionsSignature != null) {
            setter.set(carrier, XTRACE_OPTIONS_SIGNATURE, traceOptionsSignature);
        }
    }

    /**
     * Extract context from the carrier, first scanning for appoptics x-trace header.
     * If not found, try the w3c `tracestate`
     * @param context
     * @param carrier
     * @param getter
     * @param <C>
     * @return
     */
    @Override
    public <C> Context extract(Context context, @Nullable C carrier, TextMapGetter<C> getter) {
        String traceOptions = getter.get(carrier, XTRACE_OPTIONS);
        String traceOptionsSignature = getter.get(carrier, XTRACE_OPTIONS_SIGNATURE);
        XTraceOptions xTraceOptions = XTraceOptions.getXTraceOptions(traceOptions, traceOptionsSignature);
        if (xTraceOptions != null) {
            context = context.with(TriggerTraceContextKey.KEY, xTraceOptions);
            context = context.with(TriggerTraceContextKey.XTRACE_OPTIONS, traceOptions);
            if (traceOptionsSignature != null) {
                context = context.with(TriggerTraceContextKey.XTRACE_OPTIONS_SIGNATURE, traceOptionsSignature);
            }
        }
        return context;
    }
}