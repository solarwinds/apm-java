/*
 * Copyright SolarWinds Worldwide, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solarwinds.api.ext;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.opentelemetry.core.AgentState;
import java.util.concurrent.TimeUnit;

public class SolarwindsAgent {
  private SolarwindsAgent() {}

  private static final Logger logger = LoggerFactory.getLogger();

  private static boolean agentAttached = false;

  static {
    try {
      Class.forName("com.solarwinds.opentelemetry.core.CustomTransactionNameDict");
      logger.info("The SolarWinds APM agent is available.");
      agentAttached = true;

    } catch (ClassNotFoundException | NoClassDefFoundError | NoSuchMethodError e) {
      logger.warn("The SolarWinds APM Agent is not available. The SDK will be no-op.");
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
    SolarwindsAgent.agentAttached = false;
  }
}
