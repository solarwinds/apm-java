package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.core.RootSpan;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

/**
 * Span processor to keep track of the root span of a trace
 */
public class AppOpticsRootSpanProcessor implements SpanProcessor {
    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
        if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) { //then a root span of this service
            RootSpan.setRootSpan(span);
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
}
