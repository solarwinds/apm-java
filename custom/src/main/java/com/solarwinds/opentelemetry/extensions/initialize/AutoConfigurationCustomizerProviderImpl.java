package com.solarwinds.opentelemetry.extensions.initialize;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.config.JavaRuntimeVersionChecker;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.shaded.javax.annotation.Nonnull;
import com.solarwinds.opentelemetry.extensions.ResourceCustomizer;
import com.solarwinds.opentelemetry.extensions.SolarwindsPropertiesSupplier;
import com.solarwinds.opentelemetry.extensions.SolarwindsTracerProviderCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;

@AutoService({AutoConfigurationCustomizerProvider.class})
public class AutoConfigurationCustomizerProviderImpl
    implements AutoConfigurationCustomizerProvider {
  private static final Logger logger = LoggerFactory.getLogger();

  private static boolean agentEnabled;

  static {
    try {
      agentEnabled = JavaRuntimeVersionChecker.isJdkVersionSupported();
      if (agentEnabled) {
        ConfigurationLoader.load();
      } else {
        logger.warn(
            String.format(
                "Unsupported Java runtime version: %s. The lowest Java version supported is %s.",
                System.getProperty("java.version"), JavaRuntimeVersionChecker.minVersionSupported));
      }

    } catch (InvalidConfigException invalidConfigException) {
      logger.warn("Error loading agent config", invalidConfigException);
      agentEnabled = false;
    }

    if (!agentEnabled) {
      logger.warn("Solarwinds' extension is disabled");
    }
  }

  public static boolean isAgentEnabled() {
    return agentEnabled;
  }

  public static void setAgentEnabled(boolean agentEnabled) {
    AutoConfigurationCustomizerProviderImpl.agentEnabled =
        AutoConfigurationCustomizerProviderImpl.agentEnabled && agentEnabled;
  }

  @Override
  public void customize(@Nonnull AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration
        .addPropertiesSupplier(new SolarwindsPropertiesSupplier())
        .addTracerProviderCustomizer(new SolarwindsTracerProviderCustomizer())
        .addResourceCustomizer(new ResourceCustomizer());
  }

  @Override
  public int order() {
    // Here, we return Integer.MAX_VALUE to force our extension customization to execute last.
    // See https://github.com/appoptics/solarwinds-apm-java/pull/93#discussion_r1165987329 for more
    // context
    return Integer.MAX_VALUE;
  }
}
