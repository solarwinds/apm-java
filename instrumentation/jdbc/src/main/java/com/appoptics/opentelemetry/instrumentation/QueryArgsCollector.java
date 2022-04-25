package com.appoptics.opentelemetry.instrumentation;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class QueryArgsCollector {
    public static void collect(Context context, int index, Object value) {
        Span span = Span.fromContext(context);
        SpanContext spanContext = span.getSpanContext();
        if (!(spanContext.isValid() && spanContext.isSampled())) {
            return;
        }
        SortedMap<String, String> queryArgs = context.get(AoPreparedStatementInstrumentation.QueryArgsContextKey.KEY);
        if (queryArgs == null) {
            queryArgs = new TreeMap<>();
            context.with(AoPreparedStatementInstrumentation.QueryArgsContextKey.KEY, queryArgs).makeCurrent();
        }
        queryArgs.put(String.valueOf(index), JdbcEventValueConverter.convert(value).toString());
    }

    public static void maybeAttach(Context context) {
        Span span = Span.fromContext(context);
        SpanContext spanContext = span.getSpanContext();
        if ((spanContext.isValid() && spanContext.isSampled())) {
            SortedMap<String, String> argsMap = context.get(AoPreparedStatementInstrumentation.QueryArgsContextKey.KEY);

            if (argsMap != null) {
                List<String> queryArgs = new ArrayList<>(argsMap.values());
                span.setAttribute(AoPreparedStatementInstrumentation.QueryArgsAttributeKey.KEY, queryArgs);
            }
        }
    }
}
