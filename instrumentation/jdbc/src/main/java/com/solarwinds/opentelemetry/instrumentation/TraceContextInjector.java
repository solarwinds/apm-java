package com.solarwinds.opentelemetry.instrumentation;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import java.util.HashSet;
import java.util.Set;

public class TraceContextInjector {

  private static final Set<String> activeDbs = new HashSet<>();
  private static final Set<String> defaultDbs = new HashSet<>();

  static {
    defaultDbs.add("mysql");
  }

  public static String inject(Context context, String sql) {
    if (sql.contains("traceparent")) {
      return sql;
    }

    Span span = Span.fromContext(context);
    SpanContext spanContext = span.getSpanContext();
    if (!(spanContext.isValid() && spanContext.isSampled()) || doNotInjectTraceContext()) {
      return sql;
    }

    String flags = "01"; // only inject into sampled requests
    String traceContext =
        "00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-" + flags;

    String tag = String.format("/*traceparent='%s'*/", traceContext);
    span.setAttribute("QueryTag", tag);
    return String.format("%s %s", tag, sql);
  }

  public static boolean doNotInjectTraceContext() {
    Set<String> configuredDbs =
        new HashSet<>(
            ConfigManager.getConfigOptional(ConfigProperty.AGENT_SQL_TAG_DATABASES, defaultDbs));

    configuredDbs.retainAll(activeDbs);
    return configuredDbs.isEmpty();
  }

  public static void addDb(String db) {
    activeDbs.add(db);
  }
}
