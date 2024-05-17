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
