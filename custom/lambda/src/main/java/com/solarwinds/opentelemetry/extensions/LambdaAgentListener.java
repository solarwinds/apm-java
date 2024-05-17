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

import static com.solarwinds.opentelemetry.extensions.DefaultAutoConfigurationCustomizerProvider.isAgentEnabled;
import static com.solarwinds.opentelemetry.extensions.DefaultAutoConfigurationCustomizerProvider.setAgentEnabled;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.SettingsManager;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.trace.samplers.Sampler;

/**
 * Executes startup task after it's safe to do so. See <a
 * href="https://github.com/appoptics/opentelemetry-custom-distro/pull/7">...</a>
 */
@AutoService(AgentListener.class)
public class LambdaAgentListener implements AgentListener {
  private static final Logger logger = LoggerFactory.getLogger();

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk openTelemetrySdk) {
    if (isAgentEnabled() && isUsingSolarwindsSampler(openTelemetrySdk)) {
      SettingsManager.initialize(
          new AwsLambdaSettingsFetcher(new FileSettingsReader("/tmp/solarwinds-apm-settings.json")),
          SamplingConfigProvider.getSamplingConfiguration());

      TransactionNameManager.setRuntimeNameGenerator(
          spanData ->
              new TransactionNameManager.TransactionNameResult(
                  spanData
                      .getAttributes()
                      .get(AttributeKey.stringKey(SharedNames.TRANSACTION_NAME_KEY)),
                  true));
      logger.info("Successfully submitted SolarwindsAPM OpenTelemetry extensions settings");
    } else {
      logger.info("SolarwindsAPM OpenTelemetry extensions is disabled");
    }
  }

  boolean isUsingSolarwindsSampler(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    Sampler sampler =
        autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk().getSdkTracerProvider().getSampler();
    boolean verdict = sampler instanceof SolarwindsSampler;
    setAgentEnabled(verdict);

    if (!verdict) {
      logger.warn(
          "Not using Solarwinds sampler. Configured sampler is: " + sampler.getDescription());
    }
    return verdict;
  }
}
