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

package com.solarwinds.opentelemetry.extensions;

import com.solarwinds.joboe.core.EventReporter;
import com.solarwinds.joboe.core.ReporterFactory;
import com.solarwinds.joboe.core.rpc.ClientException;
import com.solarwinds.joboe.core.rpc.RpcClientManager;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;

public final class ReporterProvider {
  private ReporterProvider() {}

  private static EventReporter eventReporter;

  public static EventReporter getEventReporter() {
    if (eventReporter == null) {
      Logger logger = LoggerFactory.getLogger();
      try {
        logger.debug("Building reporter");
        eventReporter =
            ReporterFactory.getInstance()
                .createQueuingEventReporter(
                    RpcClientManager.getClient(RpcClientManager.OperationType.TRACING));
        logger.debug("Built reporter");
      } catch (ClientException clientException) {
        logger.error(
            String.format("Unable to create event reporter due error -> %s", clientException));
      }
    }
    return eventReporter;
  }
}
