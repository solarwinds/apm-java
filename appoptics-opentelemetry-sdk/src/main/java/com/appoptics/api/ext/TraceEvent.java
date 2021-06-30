package com.appoptics.api.ext;

import java.util.Map;

/**
 * Interface for trace event which is the building block of traces. Each {@link TraceEvent} has its own type such as "entry", "exit", "info" and "error", which is determined when
 * the event was created by methods in {@link Trace}. To add additional information key/value
 *
 * @see <a href="http://docs.appoptics.com/kb/apm_tracing/java/sdk/"> Java Agent - Instrumentation SDK </a>
 */
public interface TraceEvent {

    /**
     * Adds a key/value pair to an event
     * @param key   key
     * @param value value
     */
    public void addInfo(String key, Object value);

    /**
     * Adds key/value pairs to the event
     * @param infoMap  map of key/value pairs
     */
    public void addInfo(Map<String, Object> infoMap);

    /**
     * Add all key/value pairs to event. This assumes that info contains alternating key/value pairs (String, Object).
     * @param info
     */
    public void addInfo(Object... info);


    /**
     * Marks this event as asynchronous.
     * While instrumenting your code, you may want to report events from background / child threads and associate them
     * with the parent thread that spawned them. (This assumes that a trace was already started in the parent thread.)
     * You must mark these events as "async" by calling this method on the entry event associated with that background thread.
     * <p>
     * You should then call Trace.endTrace() when that thread is done processing.
     * <pre>
     * {@code
     * TraceEvent event = Trace.createEntryEvent(spanName);
     * event.setAsync();
     * event.report();
     * // Your processing ...
     * Trace.endTrace();
     * }
     * </pre>
     */
    public void setAsync();

    /**
     * Adds an additional edge to this event
     */
    public void addEdge(String xTraceID);

    /**
     * Reports the event to the collector.
     */
    public void report();

    /**
     * To report the back traces of the certain time frame of the application. This adds the back trace of the current thread to the event.
     * <pre>
     * {@code
     * TraceEvent event = Trace.createInfoEvent("some_other_layer");
     * event.addBackTrace();
     * event.report();
     * }
     * </pre>
     */
    public void addBackTrace();

}

