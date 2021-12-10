package com.appoptics.api.ext.impl;

import java.util.Map;

import com.appoptics.api.ext.TraceEvent;

public interface ITraceHandler {
    /**
     * Starts a trace, respecting the sampling rate and trace mode settings.
     *
     * @return TraceEvent: an entry event that can be populated with name/value pairs and reported
     */
    TraceEvent startTrace(String layer);

    /**
     * Continues a trace from external span, respecting the sampling rate and trace mode settings.
     *
     * @param  inXTraceID XTrace ID from incoming/previous span
     * @return TraceEvent: an entry event that can be populated with name/value pairs and reported
     */
    TraceEvent continueTrace(String layer, String inXTraceID);


    /**
     * End trace: Creates an exit event and reports it for the named span. Metadata is then cleared and the
     *            XTrace ID is returned. This is just a convenience method, as createExitEvent could also be
     *            used.
     *
     * @param layer
     * @return  XTrace ID that can be returned to calling span
     */
    String endTrace(String layer);


     /**
     * End trace: Creates an exit event and reports it for the named span. Metadata is then cleared and the
     *            XTrace ID is returned. This is just a convenience method, as createExitEvent could also be
     *            used.
     *
     * @param layer
     * @param info name/value pairs reported with exit event
     * @return  XTrace ID that can be returned to calling span
     */
     String endTrace(String layer, Map<String, Object> info);

    /**
     * End trace: Creates an exit event and reports it for the named span. Metadata is then cleared and the
     *            XTrace ID is returned. This is just a convenience method, as createExitEvent could also be
     *            used.
     *
     * @param layer
     * @param info name/value pairs reported with exit event
     * @return  XTrace ID that can be returned to calling span
     */
    String endTrace(String layer, Object... info);

    /**
     * Creates an entry event for the named span
     * @param layer
     * @return  TraceEvent: an entry event that can be populated with name/value pairs and reported
     */
    TraceEvent createEntryEvent(String layer);

    /**
     * Creates an exit event for the named span
     * @param layer
     * @return  TraceEvent: an exit event that can be populated with name/value pairs and reported
     */
    TraceEvent createExitEvent(String layer);

    /**
     * Creates an info event for the named span
     * @param layer
     * @return TraceEvent: an info event that can be populated with name/value pairs and reported
     */
    TraceEvent createInfoEvent(String layer);

    /**
     * Reports an error: creates and sends an error event for an exception (throwable), including a back trace.
     *
     * @param error  throwable to be logged
     */
    void logException(Throwable error);

    boolean setTransactionName(String transactionName);

    /**
     * Returns XTraceID associated with current context's Metadata as a hex string
     * This is suitable for propagating to other spans (HTTP -&gt; App servers, etc.)
     */
    String getCurrentXTraceID();

    /**
     * Returns a compact form of current context's Metadata suitable for logging purpose
     * @return
     */
    String getCurrentLogTraceId();
}
