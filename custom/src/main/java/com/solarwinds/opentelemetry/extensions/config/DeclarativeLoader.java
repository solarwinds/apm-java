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
import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.javaagent.tooling.BeforeAgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.internal.AutoConfigureUtil;

@AutoService(BeforeAgentListener.class)
public class DeclarativeLoader implements BeforeAgentListener {
  private static final Logger logger = LoggerFactory.getLogger();

  @Override
  public void beforeAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    ConfigProvider configProvider =
        AutoConfigureUtil.getConfigProvider(autoConfiguredOpenTelemetrySdk);

    if (configProvider != null) {
      boolean jdkVersionSupported = JavaRuntimeVersionChecker.isJdkVersionSupported();
      DeclarativeConfigProperties instrumentationConfig = configProvider.getInstrumentationConfig();

      if (instrumentationConfig != null && jdkVersionSupported) {
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
          logger.info("Loaded via declarative config");

        } catch (InvalidConfigException e) {
          throw new RuntimeException(e);
        }
      }

      if (!jdkVersionSupported) {
        logger.warn(
            String.format(
                "Unsupported Java runtime version: %s. The lowest Java version supported is %s.",
                System.getProperty("java.version"), JavaRuntimeVersionChecker.minVersionSupported));

        logger.warn("Solarwinds' extension is disabled");
      }
    }
  }
}
