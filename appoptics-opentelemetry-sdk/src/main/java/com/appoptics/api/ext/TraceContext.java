package com.appoptics.api.ext;

import com.appoptics.api.ext.impl.ITraceContextHandler;

/**
 * Allows explicit control on tracing context, which is used to manage the linkage of {@code TraceEvent}s.
 * <p>
 * This determines the current state/context of tracing, when {@code TraceEvent}s are reported, the context would be updated.
 * The context then provides references such that new events created can link and point to previous events to form extents/trace.
 * <p>
 * Under most circumstances, context is set and managed automatically.
 * This explicit control might only be necessary for trace that crosses multiple threads with top extent thread not being the
 * parent thread of all the other events.
 * <p>
 * Example:
 * <pre>
 * {@code
 * TraceContext currentContext = TraceContext.getDefault();
 * threadPoolExecutor.submit(new MyCallable(currentContext));
 * ...
 * private class MyCallable {
 *     private TraceContext traceContext;
 *     private MyCallable(TraceContext context) {
 *         this.traceContext = context;
 *     }
 *
 *     public call() {
 *         if (traceContext != null) {
 *             traceContext.setAsDefault();
 *             TraceEvent entryEvent = Trace.createEntryEvent("ThreadedJob");
 *             ...
 *         }
 *     }
 *
 * }
 *
 * }
 * </pre>
 *
 * @see com.tracelytics.joboe.Context
 */
public abstract class TraceContext {
    private static final ITraceContextHandler HANDLER = HandlerFactory.getTraceContextHandler();

    /**
     * Returns the Context currently associated with this thread.
     *
     * Note that this context is Clone of the context: modifications will NOT affect the current
     * thread unless setAsDefault is called.
     *
     * @return ITraceContextHandler
     */
    public static TraceContext getDefault() {
        return HANDLER.getDefault();
    }

    /**
     * Resets the current thread's context
     */
    public static void clearDefault() {
        HANDLER.clearDefault();
    }

    /**
     * Sets the current thread's context to this context
     */
    public abstract void setAsDefault();

    public static boolean isSampled(String xTraceID) {
        return HANDLER.isSampled(xTraceID);
    }
}
