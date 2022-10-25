package com.appoptics.api.ext;

import com.appoptics.opentelemetry.core.CustomTransactionNameDict;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;

import java.util.logging.Logger;

/**
 * The API to set the custom transaction name for the current trace. It returns false if the current trace is not valid
 * or not sampled.
 */
public class CustomTransactionName {
    private static final Logger logger = Logger.getLogger(CustomTransactionName.class.getName());
    private static boolean isAgentReady = false;
    static {
        try {
            Class.forName("com.appoptics.opentelemetry.core.CustomTransactionNameDict");
            isAgentReady = true;
            logger.info("The SolarWinds APM agent and the SDK is available.");
        } catch (ClassNotFoundException | NoClassDefFoundError | NoSuchMethodError e) {
            logger.warning("The SolarWinds APM Agent is not available. The SDK will be no-op.");
        }
    }

    /** Set the transaction name of the current trace.
     *
     * @param name  the custom transaction name to be set to the current trace
     * @return  {@code true} if the transaction name is successfully set, or {@code false} if the transaction name is not set because the span is invalid or the not sampled.
     */
    public static boolean set(String name) {
        if (!isAgentReady) {
            return false;
        }

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
