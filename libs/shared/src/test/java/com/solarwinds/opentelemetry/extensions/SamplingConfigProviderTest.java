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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.sampling.TraceConfigs;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SamplingConfigProviderTest {
  private final TraceConfigs transactionSettings = new TraceConfigs(Collections.emptyMap());

  private final TraceConfigs urlSampleRates = new TraceConfigs(Collections.emptyMap());

  @AfterEach
  void tearDown() {
    ConfigManager.reset();
  }

  @Test
  void returnsTransactionSettingsWhenBothAreDefined() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_TRANSACTION_SETTINGS, transactionSettings);
    ConfigManager.setConfig(ConfigProperty.AGENT_URL_SAMPLE_RATE, urlSampleRates);

    assertSame(transactionSettings, SamplingConfigProvider.resolveTransactionSettings());
  }

  @Test
  void returnsTransactionSettingsWhenOnlyTransactionSettingsIsDefined()
      throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_TRANSACTION_SETTINGS, transactionSettings);

    assertSame(transactionSettings, SamplingConfigProvider.resolveTransactionSettings());
  }

  @Test
  void fallsBackToUrlSampleRatesWhenTransactionSettingsIsNotDefined()
      throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_URL_SAMPLE_RATE, urlSampleRates);

    assertSame(urlSampleRates, SamplingConfigProvider.resolveTransactionSettings());
  }

  @Test
  void returnsNullWhenNeitherIsDefined() {
    assertNull(SamplingConfigProvider.resolveTransactionSettings());
  }
}
