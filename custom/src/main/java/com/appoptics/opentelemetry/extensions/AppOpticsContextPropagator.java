package com.appoptics.opentelemetry.extensions;

import com.tracelytics.instrumentation.HeaderConstants;
import com.tracelytics.joboe.XTraceOptions;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AppOpticsContextPropagator implements TextMapPropagator {
    private static final String TRACE_STATE_APPOPTICS_KEY = "sw";
    static final String TRACE_PARENT = "traceparent";
    static final String TRACE_STATE = "tracestate";
    static final String X_TRACE_OPTIONS = "X-Trace-Options";
    static final String X_TRACE_OPTIONS_SIGNATURE = "X-Trace-Options-Signature";
    private static final List<String> FIELDS =
            Collections.unmodifiableList(Arrays.asList(TRACE_PARENT, TRACE_STATE, HeaderConstants.W3C_TRACE_CONTEXT_HEADER));
    private static final int TRACESTATE_MAX_SIZE = 512;
    private static final int TRACESTATE_MAX_MEMBERS = 32;
    private static final int OVERSIZE_ENTRY_LENGTH = 129;
    private static final String TRACESTATE_KEY_VALUE_DELIMITER = "=";
    private static final String TRACESTATE_ENTRY_DELIMITER = ",";

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
        String swTraceStateValue = spanContext.getSpanId() + "-" + (spanContext.isSampled() ? "01" : "00");
        setter.set(carrier, TRACE_STATE, updateTraceState(traceState, swTraceStateValue));

        String traceOptions = context.get(TriggerTraceContextKey.XTRACE_OPTIONS);
        String traceOptionsSignature = context.get(TriggerTraceContextKey.XTRACE_OPTIONS_SIGNATURE);
        if (traceOptions != null) {
            setter.set(carrier, X_TRACE_OPTIONS, traceOptions);
        }
        if (traceOptionsSignature != null) {
            setter.set(carrier, X_TRACE_OPTIONS_SIGNATURE, traceOptionsSignature);
        }
    }

    /**
     * Update tracestate with the new SW tracestate value and do some truncation if needed.
     * @param traceState
     * @param swTraceStateValue
     * @return
     */
    private String updateTraceState(TraceState traceState, String swTraceStateValue) {
        StringBuilder traceStateBuilder = new StringBuilder(TRACESTATE_MAX_SIZE);
        traceStateBuilder.append(TRACE_STATE_APPOPTICS_KEY).append(TRACESTATE_KEY_VALUE_DELIMITER).append(swTraceStateValue);
        AtomicInteger count = new AtomicInteger(1);

        // calculate current length of the tracestate
        AtomicInteger traceStateLength = new AtomicInteger(0);
        traceState.forEach(
                (key, value) -> {
                    if (!TRACE_STATE_APPOPTICS_KEY.equals(key)
                    && ! TraceStateSamplingResult.SW_XTRACE_OPTIONS_RESP_KEY.equals(key)) {
                        traceStateLength.addAndGet(key.length());
                        traceStateLength.addAndGet(TRACESTATE_KEY_VALUE_DELIMITER.length());
                        traceStateLength.addAndGet(value.length());
                        traceStateLength.addAndGet(TRACESTATE_ENTRY_DELIMITER.length());
                    }
                }
        );

        AtomicBoolean truncateLargeEntry = new AtomicBoolean(traceStateLength.get() + traceStateBuilder.length() > TRACESTATE_MAX_SIZE);
        traceState.forEach(
                (key, value) -> {
                    if (!TRACE_STATE_APPOPTICS_KEY.equals(key)
                            && !TraceStateSamplingResult.SW_XTRACE_OPTIONS_RESP_KEY.equals(key)
                            && count.get() < TRACESTATE_MAX_MEMBERS
                            && traceStateBuilder.length() + TRACESTATE_ENTRY_DELIMITER.length() + key.length() + TRACESTATE_KEY_VALUE_DELIMITER.length() + value.length() <= TRACESTATE_MAX_SIZE) {
                        if (key.length() + TRACESTATE_KEY_VALUE_DELIMITER.length() + value.length() >= OVERSIZE_ENTRY_LENGTH
                        && truncateLargeEntry.get()) {
                            truncateLargeEntry.set(false); // only truncate one oversize entry as SW tracestate entry is smaller than OVERSIZE_ENTRY_LENGTH
                        } else {
                            traceStateBuilder.append(TRACESTATE_ENTRY_DELIMITER)
                                    .append(key)
                                    .append(TRACESTATE_KEY_VALUE_DELIMITER)
                                    .append(value);
                            count.incrementAndGet();
                        }
                    }
                });
        return traceStateBuilder.toString();
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
        String traceOptions = getter.get(carrier, X_TRACE_OPTIONS);
        String traceOptionsSignature = getter.get(carrier, X_TRACE_OPTIONS_SIGNATURE);
        XTraceOptions xTraceOptions = XTraceOptions.getXTraceOptions(traceOptions, traceOptionsSignature);
        if (xTraceOptions != null) {
            context = context.with(TriggerTraceContextKey.KEY, xTraceOptions);
            context = context.with(TriggerTraceContextKey.XTRACE_OPTIONS, traceOptions);
            if (traceOptionsSignature != null) {
                context = context.with(TriggerTraceContextKey.XTRACE_OPTIONS_SIGNATURE, traceOptionsSignature);
            }
        }

        String traceState = getter.get(carrier, TRACE_STATE);
        if (traceState != null) {
            context = context.with(TraceStateKey.KEY, traceState);
        }
        return context;
    }
}