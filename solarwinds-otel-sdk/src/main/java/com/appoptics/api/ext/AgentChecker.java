package com.appoptics.api.ext;

import com.appoptics.opentelemetry.core.AgentState;
import io.opentelemetry.javaagent.bootstrap.AgentInitializer;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  Checker on the OT/AO agent or extension status. Provide a method to block until it's ready (ie ready to trace - remote trace settings are downloaded)
 *
 * @author pluk
 */
public class AgentChecker {
    private static final Logger LOGGER = Logger.getLogger("solarwinds-sdk");
    private static boolean IS_EXTENSION_AVAILABLE = false;

    static {
        String readServiceKey = null;
        try {
            Class.forName("com.appoptics.opentelemetry.core.AgentState");
            setIsExtensionAvailable(true); //TODO version check?
        }
        catch (ClassNotFoundException e) {
            //perhaps running in OT agent environment, try agent classloader instead
            try {
                AgentInitializer.getExtensionsClassLoader().loadClass("com.appoptics.opentelemetry.core.AgentState");
                setIsExtensionAvailable(true); //TODO version check?
            }
            catch (Throwable e2) {
                LOGGER.log(Level.INFO, "Solarwinds APM extensions not available");
            }
        }
        catch (NoClassDefFoundError e) {
            /* This is not so expected as ClassLoader is supposed to throw ClassNotFoundException, but some loaders might throw NoClassDefFoundError instead */
            LOGGER.log(Level.INFO, "Solarwinds APM extensions not available");
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
        if (isExtensionAvailable()) {
            return AgentState.waitForReady(timeout, unit);
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
