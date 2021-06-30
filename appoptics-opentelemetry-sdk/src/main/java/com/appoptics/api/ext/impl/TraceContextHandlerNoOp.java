package com.appoptics.api.ext.impl;

import com.appoptics.api.ext.TraceContext;

public class TraceContextHandlerNoOp implements ITraceContextHandler {

    public TraceContext getDefault() {
        return new TraceContextNoOp();
    }

    public void clearDefault() {
    }
    
    @Override
    public boolean isSampled(String xTraceID) {
        return false;
    }

    class TraceContextNoOp extends TraceContext {
        /**
         * Sets the current thread's context to this context (updates TLS)
         */
        public void setAsDefault() {
            //Do nothing
        }
    }
}
