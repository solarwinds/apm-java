package com.solarwinds.opentelemetry.extensions;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.sampling.SamplingConfiguration;
import com.solarwinds.joboe.sampling.TraceConfigs;
import com.solarwinds.joboe.sampling.TracingMode;

public final class SamplingConfigProvider {
  private SamplingConfigProvider() {}

  private static SamplingConfiguration samplingConfiguration;

  public static SamplingConfiguration getSamplingConfiguration() {
    if (samplingConfiguration == null) {
      SamplingConfiguration.SamplingConfigurationBuilder samplingConfigurationBuilder =
          SamplingConfiguration.builder()
              .sampleRate((Integer) ConfigManager.getConfig(ConfigProperty.AGENT_SAMPLE_RATE))
              .tracingMode((TracingMode) ConfigManager.getConfig(ConfigProperty.AGENT_TRACING_MODE))
              .internalTransactionSettings(
                  (TraceConfigs)
                      ConfigManager.getConfig(ConfigProperty.AGENT_INTERNAL_TRANSACTION_SETTINGS))
              .triggerTraceEnabled(
                  ConfigManager.getConfigOptional(
                      ConfigProperty.AGENT_TRIGGER_TRACE_ENABLED, true));

      if (ConfigManager.getConfig(ConfigProperty.AGENT_CONTEXT_TTL) != null) {
        samplingConfigurationBuilder.ttl(
            (int) ConfigManager.getConfig(ConfigProperty.AGENT_CONTEXT_TTL));
      }

      if (ConfigManager.getConfig(ConfigProperty.AGENT_CONTEXT_MAX_EVENTS) != null) {
        samplingConfigurationBuilder.maxEvents(
            (int) ConfigManager.getConfig(ConfigProperty.AGENT_CONTEXT_MAX_EVENTS));
      }

      if (ConfigManager.getConfig(ConfigProperty.AGENT_CONTEXT_MAX_BACKTRACES) != null) {
        samplingConfigurationBuilder.maxBacktraces(
            (int) ConfigManager.getConfig(ConfigProperty.AGENT_CONTEXT_MAX_BACKTRACES));
      }

      samplingConfiguration = samplingConfigurationBuilder.build();
    }
    return samplingConfiguration;
  }
}
