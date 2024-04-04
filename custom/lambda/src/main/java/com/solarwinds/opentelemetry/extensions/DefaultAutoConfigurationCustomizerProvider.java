package com.solarwinds.opentelemetry.extensions;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.config.JavaRuntimeVersionChecker;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;

@AutoService({AutoConfigurationCustomizerProvider.class})
public class DefaultAutoConfigurationCustomizerProvider
    implements AutoConfigurationCustomizerProvider {
  private static final Logger logger = LoggerFactory.getLogger();

  private static boolean agentEnabled;

  static {
    try {
      agentEnabled = JavaRuntimeVersionChecker.isJdkVersionSupported();
      if (agentEnabled) {
        LambdaConfigurationLoader.load();
      } else {
        logger.warn(
            String.format(
                "Unsupported Java runtime version: %s", System.getProperty("java.version")));
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
    DefaultAutoConfigurationCustomizerProvider.agentEnabled =
        DefaultAutoConfigurationCustomizerProvider.agentEnabled && agentEnabled;
  }

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration
        .addPropertiesSupplier(new PropertiesSupplier())
        .addTracerProviderCustomizer(new TraceProviderCustomizer())
        .addResourceCustomizer(new ResourceCustomizer())
        .addMetricExporterCustomizer(new MetricExporterCustomizer());
  }

  @Override
  public int order() {
    // Here, we return Integer.MAX_VALUE to force our extension customization to execute last.
    // See https://github.com/appoptics/solarwinds-apm-java/pull/93#discussion_r1165987329 for more
    // context
    return Integer.MAX_VALUE;
  }
}
