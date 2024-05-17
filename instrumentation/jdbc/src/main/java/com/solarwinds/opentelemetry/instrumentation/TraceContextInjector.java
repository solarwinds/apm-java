/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solarwinds.opentelemetry.instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import java.util.HashSet;
import java.util.Set;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class TraceContextInjector {

  private static final Set<String> defaultDbs = new HashSet<>();

  static {
    defaultDbs.add(Db.mysql.name());
  }

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
    String traceContext =
        "00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-" + flags;

    String tag = String.format("/*traceparent='%s'*/", traceContext);
    span.setAttribute("QueryTag", tag);
    return String.format("%s %s", tag, sql);
  }

  public static boolean isDbConfigured(Db db) {
    Set<String> configuredDbs =
        new HashSet<>(
            ConfigManager.getConfigOptional(ConfigProperty.AGENT_SQL_TAG_DATABASES, defaultDbs));
    return configuredDbs.contains(db.name());
  }

  public static ElementMatcher.Junction<TypeDescription> buildMatcher() {
    ElementMatcher.Junction<TypeDescription> matcher = null;
    if (isDbConfigured(TraceContextInjector.Db.mysql)) {
      matcher = nameStartsWith("com.mysql.cj.jdbc"); // only inject MySQL JDBC driver
    }

    if (isDbConfigured(TraceContextInjector.Db.postgresql)) {
      if (matcher != null) {
        matcher = matcher.or(nameStartsWith("org.postgresql"));
      } else {
        matcher = nameStartsWith("org.postgresql");
      }
    }

    return matcher;
  }

  public enum Db {
    mysql,
    postgresql
  }
}
