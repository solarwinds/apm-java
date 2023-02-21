package com.appoptics.opentelemetry.instrumentation;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;

public class TraceContextInjector {
    public static String inject(Context context, String sql) {
        if (sql.contains("traceparent")) {
            return sql;
        }

        Span span = Span.fromContext(context);
        SpanContext spanContext = span.getSpanContext();
        if (!(spanContext.isValid() && spanContext.isSampled())) {
            return sql;
        }
        String flags = "01"; // only inject into sampled requests
        String traceContext = "00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-" + flags;
        String tag = String.format("/*traceparent='%s'*/", traceContext);
        span.setAttribute("QueryTag", tag);
        return String.format("%s %s", tag, sql);
    }
}
