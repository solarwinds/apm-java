package com.appoptics.api.ext;

import java.util.logging.Logger;

public class SolarWindsAgent {
    private SolarWindsAgent() {
    }

    private static final Logger logger = Logger.getLogger(Transaction.class.getName());

    private static boolean isAgentReady = false;

    static {
        try {
            Class.forName("com.appoptics.opentelemetry.core.CustomTransactionNameDict");
            isAgentReady = true;
            logger.info("The SolarWinds APM agent and the SDK is available.");
        } catch (ClassNotFoundException | NoClassDefFoundError | NoSuchMethodError e) {
            logger.warning("The SolarWinds APM Agent is not available. The SDK will be no-op.");
        }
    }

    public static boolean setTransactionName(String transactionName) {
        if (!isAgentReady) {
            return false;
        }
        return Transaction.setName(transactionName);
    }
}
