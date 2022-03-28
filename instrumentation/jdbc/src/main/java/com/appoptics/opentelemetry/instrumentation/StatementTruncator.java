package com.appoptics.opentelemetry.instrumentation;

import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.logging.Logger;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import com.tracelytics.logging.LoggerFactory;


public class StatementTruncator {
    private static final Logger logger = LoggerFactory.getLogger();
    public static final int DEFAULT_SQL_MAX_LENGTH = 128 * 1024; //control the max length of the SQL string to avoid BufferOverFlowException
    private static final int sqlMaxLength = ConfigManager.getConfigOptional(ConfigProperty.AGENT_SQL_QUERY_MAX_LENGTH, DEFAULT_SQL_MAX_LENGTH);

    public static void maybeTruncateStatement(Span span) {
        if (span instanceof ReadableSpan) {
            ReadableSpan readableSpan = (ReadableSpan) span;
            String sql = readableSpan.getAttribute(SemanticAttributes.DB_STATEMENT);
            if (sql == null) return;

            if (sql.length() > sqlMaxLength) {
                sql = sql.substring(0, sqlMaxLength);
                span.setAttribute(QueryTruncatedAttributeKey.KEY, true);
                span.setAttribute(SemanticAttributes.DB_STATEMENT, sql);
                logger.debug("SQL Query trimmed as its length [" + sql.length() + "] exceeds max [" + sqlMaxLength + "]");
            }
        }
    }


    public static class QueryTruncatedAttributeKey {
        public static final AttributeKey<Boolean> KEY = AttributeKey.booleanKey("QueryTruncated");
    }
}
