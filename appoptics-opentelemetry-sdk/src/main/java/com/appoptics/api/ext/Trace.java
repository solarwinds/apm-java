package com.appoptics.api.ext;

import java.util.Map;

import com.appoptics.api.ext.impl.ITraceHandler;

/**
 * API for initiating traces and creating events, for use by non-web / background apps, etc.
 * <p>
 * A Trace is a unit of instrumented work, usually mapped to a web request.
 * <p>
 * Each trace consists of exactly one top span, more spans can be added below and each of those spans can contain
 * multiple "spans" which are marked by exactly one entry and exit event and optionally info events in between.
 * A span usually represents duration of an operation.
 *
 * @see <a href="http://docs.appoptics.com/kb/apm_tracing/java/sdk/">Java Agent - Instrumentation SDK </a>
 */
public class Trace {
    private static final ITraceHandler HANDLER = HandlerFactory.getTraceHandler();

    private Trace() { } // do not allow instantiation of this class

    /**
     * Starts a trace, respecting the sampling rate.
     * <p>
     * To start a trace, you must call the startTrace method with your span name. This would typically be done when a new request enters your system.
     * You can then add key / value pairs to the event containing information about the request.
     * <p>
     * <b>Take note that this method does not report the event.</b> The {@code TraceEvent} must be reported by invoking {@link TraceEvent#report()}.
     * <p>
     * Example:
     * <pre>
     * {@code
     * TraceEvent event = Trace.startTrace("my_layer");
     * event.addInfo("my_key", "my_value");
     * event.report();
     * }
     * </pre>
     * Note that startTrace automatically takes into account sampling settings - for testing, set tracingMode to always and
     * sampleRate to 1000000. (See Configuring Java Instrumentation.)
     *
     * @param layer The name of the top span
     * @return TraceEvent: an entry {@link TraceEvent} that can be populated with name/value pairs and reported. This event
     * is the entry of the top extent.
     */
    public static TraceEvent startTrace(String layer) {
        return HANDLER.startTrace(layer);
    }

    /**
     * Continues a trace from external span, respecting the sampling rate.
     * <p>
     * If your application receive requests through a higher layer, such as an instrumented web server,
     * you will receive an identifier for that trace. This identifier, the X-Trace ID, should be provided to the continueTrace
     * method along with the span name.
     * If you are instrumenting a standalone application, you will not need to use this call. Note that you should not use
     * startTrace in this case.
     * <p>
     * <b>Take note that this method does not report the event.</b> The {@code TraceEvent} must be reported by invoking
     * {@link TraceEvent#report()}.
     * <p>
     * Example:
     * <pre>
     * {@code
     * TraceEvent event= Trace.continueTrace("my_layer", xTraceID);
     * event.addInfo("my_key", "my_value");
     * event.report();
     * }
     * </pre>
     *
     * @param layer The name of the span added below the existing span above
     * @param  inXTraceID XTrace ID from incoming/previous span
     * @return TraceEvent: an entry {@link TraceEvent} that can be populated with name/value pairs and reported. This event
     * is the entry of the extent added below the external span
     */
    public static TraceEvent continueTrace(String layer, String inXTraceID) {
        return HANDLER.continueTrace(layer, inXTraceID);
    }


    /**
     * Ends a trace by creating an exit event and reporting it for the named span. Metadata is then cleared and the
     * XTrace ID is returned. This is just a convenience method, as createExitEvent could also be used.
     * <p>
     * To end a trace, you must call the endTrace method with your span name. This would typically be done when your request
     * is done processing.
     * The X-Trace ID of the reported event is returned. If you are returning control to a higher span (such as an instrumented
     * web server),
     * you will need to return that ID using the appropriate method (such as an HTTP response header).
     * If you are tracing a standalone application, it can be ignored.
     * <p>
     * Example:
     * <pre>
     * {@code
     * String xTraceID = Trace.endTrace("my_layer");
     * }
     * </pre>
     *
     * @param layer The name of the trace's top span
     * @return  XTrace ID that can be returned to calling span
     */
    public static String endTrace(String layer) {
        return HANDLER.endTrace(layer);
    }


     /**
     * Ends a trace by creating an exit event and reporting it for the named span. Metadata is then cleared and the
     * XTrace ID is returned. This is just a convenience method, as createExitEvent could also be used.
     * <p>
     * To end a trace, you must call the endTrace method with your span name. This would typically be done when your
      * request is done processing.
     * The X-Trace ID of the reported event is returned. If you are returning control to a higher span (such as an
      * instrumented web server),
     * you will need to return that ID using the appropriate method (such as an HTTP response header).
     * If you are tracing a standalone application, it can be ignored.
     * <p>
     * Example:
     * <pre>
     * {@code
     * String xTraceID = Trace.endTrace("my_layer");
     * }
     * </pre>
     *
     * @param layer The name of the trace's top span
     * @param info name/value pairs reported with exit event
     * @return  XTrace ID that can be returned to calling span
     */
    public static String endTrace(String layer, Map<String, Object> info) {
        return HANDLER.endTrace(layer, info);
    }

    /**
     * Ends a trace by creating an exit event and reporting it for the named span. Metadata is then cleared and the
     * XTrace ID is returned. This is just a convenience method, as createExitEvent could also be used.
     * <p>
     * To end a trace, you must call the endTrace method with your span name. This would typically be done when your
     * request is done processing.
     * The X-Trace ID of the reported event is returned. If you are returning control to a higher span (such as an
     * instrumented web server),
     * you will need to return that ID using the appropriate method (such as an HTTP response header).
     * If you are tracing a standalone application, it can be ignored.
     * <p>
     * Example:
     * <pre>
     * {@code
     * String xTraceID = Trace.endTrace("my_layer");
     * }
     * </pre>
     * @param layer The name of the trace's top span
     * @param info name/value pairs reported with exit event
     * @return  XTrace ID that can be returned to calling span
     */
    public static String endTrace(String layer, Object... info) {
        return HANDLER.endTrace(layer, info);
    }

    /**
     * Creates an entry event of a new span with the given name. The entry event indicates the start of the newly created span.
     *
     * It is up to you, the application developer, to decide how to segment your application's modules and subsystems into spans.
     * The event must be created, populated with name/value pairs.
     * <p>
     * <b>Take note that this method does not report the event.</b> The {@code TraceEvent} must be reported by invoking
     * {@link TraceEvent#report()}.
     * <p>
     * Example:
     * <pre>
     * {@code
     * TraceEvent event = Trace.createEntryEvent("some_other_layer");
     * event.addInfo("name2","value2");
     * event.report();
     * }
     * </pre>
     *
     * @param layer The name of the new span to be created
     * @return  TraceEvent: an entry {@link TraceEvent} that can be populated with name/value pairs and reported later
     */
    public static TraceEvent createEntryEvent(String layer) {
        return HANDLER.createEntryEvent(layer);
    }

    /**
     * Creates an exit event of the current span with the given name. The exit event indicates the end of the current span.
     *
     * It can be populated with name/value pairs just like the entry event. There should be a matching exit event for
     * each entry.
     * <p>
     * <b>Take note that this method does not report the event.</b> The {@code TraceEvent} must be reported by invoking
     * {@link TraceEvent#report()}.
     * <p>
     * Example:
     * <pre>
     * {@code
     * TraceEvent event = Trace.createExitEvent("some_other_layer");
     * event.addInfo("name1", "value1");
     * event.addInfo("name2", "value2");
     * event.report();
     * }
     * </pre>
     *
     * @param layer The name of the current span to be ended
     * @return  TraceEvent: an exit {@link TraceEvent} that can be populated with name/value pairs and reported later
     */
    public static TraceEvent createExitEvent(String layer) {
        return HANDLER.createExitEvent(layer);
    }

    /**
     * Creates an info event for the named span. You may need to report various information as your application executes,
     * in between the entry and exit events of a particular span.
     * This can be done using info events. Note that the span name of the info event can be null if you wish to inherit
     * the current span.
     * <p>
     * <b>Take note that this method does not report the event.</b> The {@code TraceEvent} must be reported by invoking
     * {@link TraceEvent#report()}.
     * <p>
     * Example:
     * <pre>
     * {@code
     * TraceEvent event = Trace.createInfoEvent("some_other_layer");
     * event.addInfo("something", "interesting");
     * event.report();
     * }
     * </pre>
     * @param layer The name of the span which the info event is created for. null if the current span name is to be used.
     * @return TraceEvent: an info event that can be populated with name/value pairs and reported
     */
    public static TraceEvent createInfoEvent(String layer) {
        return HANDLER.createInfoEvent(layer);
    }

    /**
     * Creates and sends an error event for an exception (throwable), including a back trace.
     * <p>
     * Example:
     * <pre>
     * {@code
     * try {
     *     // your code that might throw an exception goes here ...
     * } catch(YourException exception) {
     *     Trace.logException(exception);
     *     // the rest of your exception handler ...
     * }
     * }
     * </pre>
     * @param error throwable to be logged
     */
    public static void logException(Throwable error) {
        HANDLER.logException(error);
    }

    /**
     * Sets a transaction name to the current active trace, the transaction name will be reported along with the
     * corresponding trace and metrics.
     *
     * This overrides the transaction name provided by out-of-the-box instrumentation.
     *
     * If multiple transaction names are set on the same trace, then the last one would be used.
     *
     * Take note that transaction name might be truncated with invalid characters replaced.
     *
     * Empty string and null are considered invalid transaction name values and will be ignored
     *
     * @param transactionName   transaction name to be used, should not be null or empty string.
     * @return  true if there is an active trace and transaction name is not null or empty string.
     */
    public static boolean setTransactionName(String transactionName) {
        return HANDLER.setTransactionName(transactionName);
    }

    /**
     * Returns XTraceID associated with current context's Metadata as a hex string
     * This is suitable for propagating to other spans (HTTP -&gt; App servers, etc.)
     *
     * Take note that in order to correlate logs with traces, please use {@link Trace#getCurrentLogTraceID()}
     *
     * @return a full id for current context
     */
    public static String getCurrentXTraceID() {
        return HANDLER.getCurrentXTraceID();
    }
    /**
     * Returns a compact form of current context's Metadata suitable for logging purpose
     * @return a compact id for current context
     */
    public static String getCurrentLogTraceID() {
        return HANDLER.getCurrentLogTraceId();
    }
}
