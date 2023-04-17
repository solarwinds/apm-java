package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.core.RootSpan;
import com.tracelytics.joboe.XTraceOption;
import com.tracelytics.joboe.XTraceOptions;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

import javax.annotation.Nonnull;

/**
 * Span processor to keep track of the root span of a trace
 */
public class AppOpticsRootSpanProcessor implements SpanProcessor {
    @Override
    public void onStart(@Nonnull Context parentContext, @Nonnull ReadWriteSpan span) {
        SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
        if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) { //then a root span of this service
            RootSpan.setRootSpan(span);
            processXtraceOptions(parentContext, span);
        }
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        SpanContext parentSpanContext = span.toSpanData().getParentSpanContext();
        if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) { //then a root span of this service
            RootSpan.clearRootSpan(span.getSpanContext().getTraceId());
        }
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }

    private void processXtraceOptions(@Nonnull Context parentContext, @Nonnull ReadWriteSpan span) {
        XTraceOptions xTraceOptions = parentContext.get(TriggerTraceContextKey.KEY);
        if (xTraceOptions != null) {
            xTraceOptions.getCustomKvs().forEach(
                    ((stringXTraceOption, s) -> span.setAttribute(stringXTraceOption.getKey(), s)));
            if (xTraceOptions.getOptionValue(XTraceOption.TRIGGER_TRACE)) {
                span.setAttribute("TriggeredTrace", true);
            }

            String swKeys = xTraceOptions.getOptionValue(XTraceOption.SW_KEYS);
            if (swKeys != null) {
                span.setAttribute("SWKeys", swKeys);
            }
        }
    }
}
