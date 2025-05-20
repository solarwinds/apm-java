package com.solarwinds.opentelemetry.extensions.initialize.config;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.ConfigContainer;
import com.solarwinds.joboe.config.ConfigGroup;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.config.JavaRuntimeVersionChecker;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.opentelemetry.extensions.LoggingConfigProvider;
import com.solarwinds.opentelemetry.extensions.config.parsers.yaml.DeclarativeConfigParser;
import com.solarwinds.opentelemetry.extensions.initialize.AutoConfigurationCustomizerProviderImpl;
import com.solarwinds.opentelemetry.extensions.initialize.ConfigurationLoader;
import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.javaagent.tooling.BeforeAgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.internal.AutoConfigureUtil;

@AutoService(BeforeAgentListener.class)
public class ConfigLoader implements BeforeAgentListener {
  private static final Logger logger = LoggerFactory.getLogger();

  @Override
  public void beforeAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    ConfigProvider configProvider =
        AutoConfigureUtil.getConfigProvider(autoConfiguredOpenTelemetrySdk);

    boolean jdkVersionSupported = JavaRuntimeVersionChecker.isJdkVersionSupported();
    AutoConfigurationCustomizerProviderImpl.setAgentEnabled(jdkVersionSupported);

    if (configProvider != null) {
      DeclarativeConfigProperties instrumentationConfig = configProvider.getInstrumentationConfig();

      if (instrumentationConfig != null) {
        DeclarativeConfigProperties solarwinds =
            instrumentationConfig
                .getStructured("java", DeclarativeConfigProperties.empty())
                .getStructured("solarwinds", DeclarativeConfigProperties.empty());

        try {
          ConfigContainer configContainer = new ConfigContainer();
          new DeclarativeConfigParser(configContainer).parse(solarwinds);
          ConfigurationLoader.processConfigs(configContainer);

          ConfigContainer agentConfig = configContainer.subset(ConfigGroup.AGENT);
          LoggerFactory.init(LoggingConfigProvider.getLoggerConfiguration(agentConfig));
          logger.info("ConfigContainer: " + configContainer);

        } catch (InvalidConfigException e) {
          throw new RuntimeException(e);
        }
      }

    } else {
      try {
        if (AutoConfigurationCustomizerProviderImpl.isAgentEnabled()) {
          ConfigurationLoader.load();
        }

      } catch (InvalidConfigException invalidConfigException) {
        logger.warn("Error loading agent config", invalidConfigException);
        AutoConfigurationCustomizerProviderImpl.setAgentEnabled(false);
      }
    }

    if (!jdkVersionSupported) {
      logger.warn(
          String.format(
              "Unsupported Java runtime version: %s. The lowest Java version supported is %s.",
              System.getProperty("java.version"), JavaRuntimeVersionChecker.minVersionSupported));
    }

    if (!AutoConfigurationCustomizerProviderImpl.isAgentEnabled()) {
      logger.warn("Solarwinds' extension is disabled");
    }
  }
}
