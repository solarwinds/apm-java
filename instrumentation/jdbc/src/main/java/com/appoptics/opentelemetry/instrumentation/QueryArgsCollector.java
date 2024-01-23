package com.appoptics.opentelemetry.instrumentation;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class QueryArgsCollector {
  private static final Map<Statement, SortedMap<String, String>> instrumentationStore =
      new ConcurrentHashMap<>();

  public static void collect(Statement statement, Context context, int index, Object value) {
    Span span = Span.fromContext(context);
    SpanContext spanContext = span.getSpanContext();
    if (!(spanContext.isValid() && spanContext.isSampled())) {
      return;
    }

    SortedMap<String, String> queryArgs =
        instrumentationStore.computeIfAbsent(
            statement, stmt -> Collections.synchronizedSortedMap(new TreeMap<>()));
    queryArgs.put(String.valueOf(index), JdbcEventValueConverter.convert(value).toString());
  }

  public static void maybeAttach(Statement statement, Context context) {
    Span span = Span.fromContext(context);
    SpanContext spanContext = span.getSpanContext();
    if (!(spanContext.isValid() && spanContext.isSampled())) {
      return;
    }

    SortedMap<String, String> argsMap = instrumentationStore.get(statement);
    if (argsMap != null && !argsMap.isEmpty()) {
      List<String> queryArgs = new ArrayList<>(argsMap.values());
      span.setAttribute(SwoPreparedStatementInstrumentation.QueryArgsAttributeKey.KEY, queryArgs);
      argsMap.clear();
    }
    instrumentationStore.computeIfPresent(statement, (stmt, sortedMap) -> null);
  }
}
