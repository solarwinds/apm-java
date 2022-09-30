package com.appoptics.api.ext;

import com.appoptics.opentelemetry.core.CustomTransactionNameDict;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;

/**
 * The API to set the custom transaction name for the current trace. It returns false if the current trace is not valid
 * or not sampled.
 */
public class CustomTransactionName {
    public static boolean set(String name) {
        Context context = Context.current();
        Span span = Span.fromContext(context);
        SpanContext spanContext = span.getSpanContext();
        if (!(spanContext.isValid() && spanContext.isSampled())) {
            return false;
        }
        CustomTransactionNameDict.set(spanContext.getTraceId(), name);
        return true;
    }
}
