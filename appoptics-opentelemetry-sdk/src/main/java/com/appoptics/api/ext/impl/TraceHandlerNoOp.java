package com.appoptics.api.ext.impl;

import java.util.Map;

import com.appoptics.api.ext.TraceEvent;
import com.appoptics.api.ext.model.NoOpEvent;

/**
 * API for initiating traces and creating events, for use by non-web / background apps, etc.
 */
public class TraceHandlerNoOp implements ITraceHandler {
    public TraceEvent startTrace(String layer) {
        return NO_OP;
    }
    
    public TraceEvent continueTrace(String layer, String inXTraceID) {
        return NO_OP;
    }

    public String endTrace(String layer) {
        return "";
    }

    public String endTrace(String layer, Map<String, Object> info) {
        return "";
    }

    public String endTrace(String layer, Object... info) {
        return "";
    }

    public TraceEvent createEntryEvent(String layer) {
        return NO_OP;
    }

    public TraceEvent createExitEvent(String layer) {
        return NO_OP;
    }

    public TraceEvent createInfoEvent(String layer) {
        return NO_OP;
    }

    public void logException(Throwable error) {
    }

    public String getCurrentXTraceID() {
        return "";
    }
    
    @Override
    public String getCurrentLogTraceId() {
        return "";
    }

    private static final NoOpEvent NO_OP = new NoOpEvent();

    public boolean setTransactionName(String transactionName) {
        if (transactionName == null || "".equals(transactionName)) {
            return false;   
        }
        
        return true;
    }
}
