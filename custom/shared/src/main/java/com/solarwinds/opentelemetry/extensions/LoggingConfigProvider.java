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
