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

package com.solarwinds.opentelemetry.extensions.config;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.ConfigContainer;
import com.solarwinds.joboe.config.ConfigGroup;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.config.JavaRuntimeVersionChecker;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.opentelemetry.extensions.LoggingConfigProvider;
import com.solarwinds.opentelemetry.extensions.config.parser.yaml.DeclarativeConfigParser;
import io.opentelemetry.api.incubator.ExtendedOpenTelemetry;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.javaagent.tooling.BeforeAgentListener;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

@AutoService(BeforeAgentListener.class)
public class DeclarativeLoader implements BeforeAgentListener {
  private static final Logger logger = LoggerFactory.getLogger();

  @Override
  public void beforeAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    OpenTelemetrySdk openTelemetrySdk = autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk();
    if (!(openTelemetrySdk instanceof ExtendedOpenTelemetry)) {
      // Shouldn't happen in practice, but just in case
      logger.warn(
          "OpenTelemetrySdk is not an instance of ExtendedOpenTelemetry. Declarative configuration will not be applied.");
      return;
    }

    ExtendedOpenTelemetry extendedOpenTelemetry = (ExtendedOpenTelemetry) openTelemetrySdk;
    DeclarativeConfigProperties solarwinds =
        extendedOpenTelemetry.getInstrumentationConfig("solarwinds");

    if (solarwinds != null && !solarwinds.getPropertyKeys().isEmpty()) {
      try {
        ConfigContainer configContainer = new ConfigContainer();
        DeclarativeConfigParser declarativeConfigParser =
            new DeclarativeConfigParser(configContainer);

        declarativeConfigParser.parse(solarwinds);
        ConfigurationLoader.processConfigs(configContainer);

        ConfigContainer agentConfig = configContainer.subset(ConfigGroup.AGENT);
        LoggerFactory.init(LoggingConfigProvider.getLoggerConfiguration(agentConfig));
        logger.info("Loaded via declarative config");

      } catch (InvalidConfigException e) {
        throw new RuntimeException(e);
      }

      if (!JavaRuntimeVersionChecker.isJdkVersionSupported()) {
        logger.warn(
            String.format(
                "Profiling is not supported for Java runtime version: %s . The lowest Java version supported for profiling is %s.",
                System.getProperty("java.version"), JavaRuntimeVersionChecker.minVersionSupported));
      }
    }
  }
}
