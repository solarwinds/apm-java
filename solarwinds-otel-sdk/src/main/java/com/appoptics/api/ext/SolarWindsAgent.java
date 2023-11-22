package com.appoptics.api.ext;

import com.appoptics.opentelemetry.core.AgentState;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SolarWindsAgent {
  private SolarWindsAgent() {}

  private static final Logger logger = Logger.getLogger(SolarWindsAgent.class.getName());

  private static boolean agentAttached = false;

  static {
    try {
      Class.forName("com.appoptics.opentelemetry.core.CustomTransactionNameDict");
      logger.info("The SolarWinds APM agent is available.");
      agentAttached = true;

    } catch (ClassNotFoundException | NoClassDefFoundError | NoSuchMethodError e) {
      logger.warning("The SolarWinds APM Agent is not available. The SDK will be no-op.");
    }
  }

  /**
   * Set the name of the current transaction.
   *
   * @param transactionName the name of the transaction
   * @return returns true if transaction name was set
   */
  public static boolean setTransactionName(String transactionName) {
    if (agentAttached) {
      return Transaction.setName(transactionName);
    }
    return true;
  }

  @Deprecated
  public static boolean waitUntilAgentReady(long timeout, TimeUnit unit) {
    return waitUntilReady(timeout, unit);
  }

  /**
   * Blocks until agent is ready (established connection with data collector) or timeout expired.
   *
   * <p>Take note that if an agent is not ready, traces and metrics collected will not be processed.
   *
   * <p>Call this method to ensure agent is ready before reporting traces for one-off batch jobs
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout argument
   * @return returns true if the agent is ready
   */
  public static boolean waitUntilReady(long timeout, TimeUnit unit) {
    if (agentAttached) {
      return AgentState.waitForReady(timeout, unit);
    }
    return false;
  }

  // Visible for testing
  static void setAgentAttachedToFalse() {
    SolarWindsAgent.agentAttached = false;
  }
}
