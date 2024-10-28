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

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.semconv.SemanticAttributes;
import java.lang.reflect.Method;

public class StatementTruncator {
  private static final Logger logger = LoggerFactory.getLogger();
  public static final int DEFAULT_SQL_MAX_LENGTH =
      128 * 1024; // control the max length of the SQL string to avoid BufferOverFlowException

  public static void maybeTruncateStatement(Context context) {
    Span span = Span.fromContext(context);
    SpanContext spanContext = span.getSpanContext();

    if (spanContext.isValid() && spanContext.isSampled()) {
      String sql = null;
      try {
        /* Note that we cannot cast the object of class `io.opentelemetry.sdk.trace.RecordEventsReadableSpan` to interface
         * `io.opentelemetry.sdk.trace.ReadableSpan` as they are loaded by different classloaders.
         * */
        Method getAttribute = span.getClass().getDeclaredMethod("getAttribute", AttributeKey.class);
        getAttribute.setAccessible(true);
        sql = (String) getAttribute.invoke(span, SemanticAttributes.DB_STATEMENT);
      } catch (Throwable throwable) {
        logger.debug("Cannot execute method getAttribute: " + throwable);
      }
      if (sql == null) {
        return;
      }

      int sqlMaxLength =
          ConfigManager.getConfigOptional(
              ConfigProperty.AGENT_SQL_QUERY_MAX_LENGTH, DEFAULT_SQL_MAX_LENGTH);

      if (sql.contains("traceparent")) {
        // Remove trace context from captured db.statement. For hibernate, we inject context before
        // it's captured by upstream.
        sql = sql.replaceAll("/.*traceparent.*/", "");
        span.setAttribute(SemanticAttributes.DB_STATEMENT, sql);
      }

      if (sql.length() > sqlMaxLength) {
        sql = sql.substring(0, sqlMaxLength);
        span.setAttribute(QueryTruncatedAttributeKey.KEY, true);
        span.setAttribute(SemanticAttributes.DB_STATEMENT, sql);
        logger.debug(
            "SQL Query trimmed as its length ["
                + sql.length()
                + "] exceeds max ["
                + sqlMaxLength
                + "]");
      }
    }
  }

  public static class QueryTruncatedAttributeKey {
    public static final AttributeKey<Boolean> KEY = AttributeKey.booleanKey("QueryTruncated");
  }
}
