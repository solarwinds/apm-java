package com.appoptics.api.ext.impl;

import com.appoptics.api.ext.TraceContext;

public interface ITraceContextHandler {
    /**
     * Returns the Context currently associated with this thread.
     *
     * Note that this context is a copy of the TLS context: modifications will NOT affect the current
     * thread unless setAsDefault is called.
     *
     * @return ITraceContextHandler
     */
    public TraceContext getDefault();
    
    /**
     * Resets the current thread's context (updates TLS)
     */
    public void clearDefault();
    
    /**
     * Returns whether the xTraceID is from a sampled request
     * @param xTraceID
     * @return
     */
    public boolean isSampled(String xTraceID);
}
