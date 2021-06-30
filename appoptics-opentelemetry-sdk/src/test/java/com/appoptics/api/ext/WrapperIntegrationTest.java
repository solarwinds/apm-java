package com.appoptics.api.ext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;

import junit.framework.TestCase;

public class WrapperIntegrationTest extends TestCase {
    private Logger logger = Logger.getAnonymousLogger();
    
    public void testGet() throws IOException {
        String result = getUrl(new URL("http://localhost:8080/test.jsp"));
        
//        assertTrue("Cannot find the RUM header in the response:[" + result + "]", result.contains("var b=this._tly"));
//        assertTrue("Cannot find the RUM footer in the response:[" + result + "]", result.contains("this._tly&&this._tly.measure(\"domload\");"));
        
        //in T2, we no longer add RUM header, ensure the response does not contain the RUM header
        assertFalse("Still found RUM header in the response:[" + result + "]", result.contains("var b=this._tly"));
        assertFalse("Still found RUM footer in the response:[" + result + "]", result.contains("this._tly&&this._tly.measure(\"domload\");"));
    }
    
    private static String getUrl(URL url) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        
        String inLine;
        StringBuffer result = new StringBuffer();
        while ((inLine = in.readLine()) != null) {
           result.append(inLine);
        }
        in.close();
        
        return result.toString();
    }
}
