package com.appoptics.api.ext;

import com.appoptics.api.ext.impl.IRumHandler;

/**
 * RUM (real user monitoring) is no longer supported in this SDK
 * 
 * @author pluk
 *
 */
@Deprecated
public class RUM {
    private static IRumHandler handler = HandlerFactory.getRumHandler();
    
    /**
     * @deprecated Manual RUM is no longer supported 
     */
    public static String getHeader() {
        return handler.getHeader();
    }

    /**
     * @deprecated Manual RUM is no longer supported
     */
    public static String getFooter() {
        return handler.getFooter();
    }
}
