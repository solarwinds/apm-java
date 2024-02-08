package com.solarwinds.opentelemetry.extensions.initialize;

import com.solarwinds.opentelemetry.extensions.SolarwindsPropertiesSupplier;
import com.solarwinds.opentelemetry.extensions.SolarwindsTracerProviderCustomizer;
import com.appoptics.opentelemetry.extensions.lambda.MetricExporterCustomizer;
import com.solarwinds.opentelemetry.extensions.lambda.PropertiesSupplier;
import com.solarwinds.opentelemetry.extensions.lambda.RuntimeTraceProviderCustomizer;
import com.google.auto.service.AutoService;
import com.solarwinds.joboe.core.config.InvalidConfigException;
import com.solarwinds.joboe.core.logging.Logger;
import com.solarwinds.joboe.core.logging.LoggerFactory;
import com.solarwinds.joboe.core.util.JavaRuntimeVersionChecker;
import com.solarwinds.joboe.shaded.javax.annotation.Nonnull;
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
    AutoConfigurationCustomizerProviderImpl.agentEnabled =
        AutoConfigurationCustomizerProviderImpl.agentEnabled && agentEnabled;
  }

  @Override
  public void customize(@Nonnull AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration
        .addPropertiesSupplier(new PropertiesSupplier(new SolarwindsPropertiesSupplier()))
        .addTracerProviderCustomizer(
            new RuntimeTraceProviderCustomizer(new SolarwindsTracerProviderCustomizer()))
        .addResourceCustomizer(new AutoConfiguredResourceCustomizer())
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
