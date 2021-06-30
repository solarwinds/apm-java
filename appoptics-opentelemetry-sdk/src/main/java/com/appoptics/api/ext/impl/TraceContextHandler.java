package com.appoptics.api.ext.impl;

import com.appoptics.api.ext.TraceContext;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.OboeException;
import com.tracelytics.joboe.span.impl.ScopeContextSnapshot;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

public class TraceContextHandler implements ITraceContextHandler {
    private final Logger logger = LoggerFactory.getLogger();
    private Scope currentScope;
    /**
     * Returns the Context currently associated with this thread.
     *
     * Note that this context is a copy of the TLS context: modifications will NOT affect the current
     * thread unless setAsDefault is called.
     *
     * @return ITraceContextHandler
     */
    public TraceContext getDefault() {
        return new TraceContextConcrete(Span.current());
    }

    /**
     * Resets the current thread's context (updates TLS)
     */
    public void clearDefault() {
        if (currentScope != null) {
            currentScope.close();
            currentScope = null;
        }
    }
    
    @Override
    public boolean isSampled(String xTraceID) {
        try {
            return new Metadata(xTraceID).isSampled();
        } catch (OboeException e) {
            logger.warn("X-Trace ID [" + xTraceID + "] is not valid");
            return false;
        }
    }
    
    
    class TraceContextConcrete extends TraceContext {
        private final Span span;

        /**
         * Constructor: Currently, the only way public way to obtain a ITraceContextHandler is through getDefault
         */
        protected TraceContextConcrete(Span span) {
            this.span = span;
        }
        
        @Override
        /**
         * Sets the current thread's context to this context (updates TLS)
         * 
         * Take note that if current thread does not invoke {@link com.appoptics.api.ext.Trace#endTrace} to end the trace,
         * then it is encouraged to invoke {@link TraceContext#clearDefault()} to clear up the context after the processing is done on current thread.
         */
        public void setAsDefault() {
            if (span != null) {
                currentScope = io.opentelemetry.context.Context.current().with(span).makeCurrent();
            }
        }
    }
}
