package com.appoptics.api.ext;

import com.appoptics.api.ext.impl.IRumHandler;
import com.appoptics.api.ext.impl.ITraceContextHandler;
import com.appoptics.api.ext.impl.ITraceHandler;
import com.appoptics.api.ext.impl.RUMHandlerNoOp;
import com.appoptics.api.ext.impl.TraceContextHandler;
import com.appoptics.api.ext.impl.TraceContextHandlerNoOp;
import com.appoptics.api.ext.impl.TraceHandler;
import com.appoptics.api.ext.impl.TraceHandlerNoOp;

/**
 * Creates handler for various existing sdk classes that provide static methods. Depending on whether the java agent is available and up-to-date, no ops handlers
 * or normal handler would be returned.
 * 
 *  
 * @author Patson Luk
 *
 */
class HandlerFactory {
    private HandlerFactory() {
    }
    
    static ITraceHandler getTraceHandler() {
        return AgentChecker.isExtensionAvailable ? new TraceHandler() : new TraceHandlerNoOp();
    }

    public static ITraceContextHandler getTraceContextHandler() {
        return AgentChecker.isExtensionAvailable ? new TraceContextHandler() : new TraceContextHandlerNoOp();
    }

    public static IRumHandler getRumHandler() {
        return new RUMHandlerNoOp(); //always returns noop as manual RUM is no longer supported in T2
    }
}
