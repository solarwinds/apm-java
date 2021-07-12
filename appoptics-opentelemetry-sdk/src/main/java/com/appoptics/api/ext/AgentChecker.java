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
    private static Logger logger = Logger.getLogger("appoptics-sdk");
    private static final String APPOPTICS_SERVICE_KEY = "otel.appoptics.service.key";
    static boolean isExtensionAvailable = false;
    private static final String serviceKey;

    static {
        String readServiceKey = null;
        try {
            Class.forName("com.appoptics.opentelemetry.extensions.initialize.Initializer");
            isExtensionAvailable = true; //TODO version check?
            readServiceKey = System.getProperty(APPOPTICS_SERVICE_KEY);
        } catch (ClassNotFoundException e) {
            //perhaps running in OT agent environment, try agent classloader instead
            try {
                AgentInitializer.getAgentClassLoader().loadClass("com.appoptics.opentelemetry.extensions.initialize.Initializer");
                isExtensionAvailable = true; //TODO version check?
                readServiceKey = System.getProperty(APPOPTICS_SERVICE_KEY);
            } catch (Throwable e2) {
                logger.log(Level.INFO, "AppOptics extensions not available");
            }
        } catch (NoClassDefFoundError e) {
            /* This is not so expected as ClassLoader is supposed to throw ClassNotFoundException, but some loaders might throw NoClassDefFoundError instead */
            logger.log(Level.INFO, "AppOptics extensions not available");
        } finally {
            serviceKey = readServiceKey;
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
        if (isExtensionAvailable && serviceKey != null) {
            try {
                //Future<?> future = Initializer.initialize(serviceKey);
                Future<?> future = Initializer.getStartupTasksFuture();
                if (future != null) {
                    future.get(timeout, unit);
                    return true;
                } else {
                    logger.warning("AppOptics can only be used with javaagent");
                    return false;
                }

            } catch (Exception e) {
                logger.warning("Agent is still not ready after waiting for " + timeout + " " + unit);
                return false;
            }
        }
        return false;
    }
}
