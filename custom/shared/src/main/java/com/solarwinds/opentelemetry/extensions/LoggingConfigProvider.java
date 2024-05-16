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

import com.solarwinds.joboe.config.ConfigContainer;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.logging.LogSetting;
import com.solarwinds.joboe.logging.LoggerConfiguration;
import java.nio.file.Path;

public final class LoggingConfigProvider {
  private LoggingConfigProvider() {}

  public static LoggerConfiguration getLoggerConfiguration(ConfigContainer container) {
    LoggerConfiguration.LoggerConfigurationBuilder loggerConfiguration =
        LoggerConfiguration.builder();
    if (container.containsProperty(ConfigProperty.AGENT_LOGGING)) {
      loggerConfiguration.logSetting((LogSetting) container.get(ConfigProperty.AGENT_LOGGING));
    }

    if (container.containsProperty(ConfigProperty.AGENT_DEBUG)) {
      loggerConfiguration
          .debug((Boolean) container.get(ConfigProperty.AGENT_DEBUG))
          .logFile((Path) container.get(ConfigProperty.AGENT_LOG_FILE));
    }

    if (container.containsProperty(ConfigProperty.AGENT_LOG_FILE)) {
      loggerConfiguration.logFile((Path) container.get(ConfigProperty.AGENT_LOG_FILE));
    }

    return loggerConfiguration.build();
  }
}
