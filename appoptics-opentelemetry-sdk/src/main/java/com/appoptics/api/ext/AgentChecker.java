package com.appoptics.api.ext;

import com.appoptics.opentelemetry.extensions.initialize.Initializer;
import io.opentelemetry.javaagent.bootstrap.AgentInitializer;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  Checker on the OT/AO agent or extension status. Provide a method to block until it's ready (ie ready to trace - remote trace settings are downloaded)
 *
 * @author pluk
 */
public class AgentChecker {
    private static final Logger LOGGER = Logger.getLogger("appoptics-sdk");
    private static final String APPOPTICS_SERVICE_KEY = "otel.appoptics.service.key";
    private static boolean IS_EXTENSION_AVAILABLE = false;
    private static final String SERVICE_KEY;

    static {
        String readServiceKey = null;
        try {
            Class.forName("com.appoptics.opentelemetry.extensions.initialize.Initializer");
            setIsExtensionAvailable(true); //TODO version check?
            readServiceKey = System.getProperty(APPOPTICS_SERVICE_KEY);
        }
        catch (ClassNotFoundException e) {
            //perhaps running in OT agent environment, try agent classloader instead
            try {
                AgentInitializer.getExtensionsClassLoader().loadClass("com.appoptics.opentelemetry.extensions.initialize.Initializer");
                setIsExtensionAvailable(true); //TODO version check?
                readServiceKey = System.getProperty(APPOPTICS_SERVICE_KEY);
            }
            catch (Throwable e2) {
                LOGGER.log(Level.INFO, "AppOptics extensions not available");
            }
        }
        catch (NoClassDefFoundError e) {
            /* This is not so expected as ClassLoader is supposed to throw ClassNotFoundException, but some loaders might throw NoClassDefFoundError instead */
            LOGGER.log(Level.INFO, "AppOptics extensions not available");
        }
        finally {
            SERVICE_KEY = readServiceKey;
        }

    }


    /**
     * Blocks until agent is ready (established connection with data collector) or timeout expired.
     * <p>
     * Take note that if an agent is not ready, traces and metrics collected will not be processed.
     * <p>
     * Call this method to ensure agent is ready before reporting traces for one-off batch jobs
     *
     * @param timeout
     * @param unit
     * @return whether the agent is ready
     */
    public static boolean waitUntilAgentReady(long timeout, TimeUnit unit) {
        if (isExtensionAvailable() && SERVICE_KEY != null) {
            try {
                //Future<?> future = Initializer.initialize(serviceKey);
                final Future<?> future = Initializer.getStartupTasksFuture();
                if (future != null) {
                    future.get(timeout, unit);
                    return true;
                }
                else {
                    LOGGER.warning("AppOptics can only be used with javaagent");
                    return false;
                }

            }
            catch (Exception e) {
                LOGGER.warning("Agent is still not ready after waiting for " + timeout + " " + unit);
                return false;
            }
        }
        return false;
    }

    public static boolean isExtensionAvailable() {
        return IS_EXTENSION_AVAILABLE;
    }

    public static void setIsExtensionAvailable(boolean isExtensionAvailable) {
        AgentChecker.IS_EXTENSION_AVAILABLE = isExtensionAvailable;
    }
}
