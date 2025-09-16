/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
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

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

@AutoService(AgentListener.class)
public class BuildInfoLogger implements AgentListener {
  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    Logger logger = LoggerFactory.getLogger();
    logger.info(String.format("Otel agent version: %s", BuildConfig.OTEL_AGENT_VERSION));
    logger.info(
        String.format("Solarwinds extension version: %s", BuildConfig.SOLARWINDS_AGENT_VERSION));

    logger.info(String.format("Solarwinds build datetime: %s", BuildConfig.BUILD_DATETIME));
    logger.info(String.format("Your Java version: %s", System.getProperty("java.version")));
  }
}
