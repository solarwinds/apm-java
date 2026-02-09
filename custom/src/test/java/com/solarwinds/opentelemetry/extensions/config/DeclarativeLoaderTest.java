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

package com.solarwinds.opentelemetry.extensions.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.extension.incubator.ExtendedOpenTelemetrySdk;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalLanguageSpecificInstrumentationPropertyModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeclarativeLoaderTest {

  @InjectMocks private DeclarativeLoader tested;

  @Mock private AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdkMock;

  @Mock private ExtendedOpenTelemetrySdk extendedOpenTelemetrySdkMock;

  private final ExperimentalLanguageSpecificInstrumentationPropertyModel solarwinds =
      new ExperimentalLanguageSpecificInstrumentationPropertyModel()
          .withAdditionalProperty(
              ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(), "token:service")
          .withAdditionalProperty(
              ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(), "apm.collector.com");

  @AfterAll
  public static void clearConfig() {
    ConfigManager.reset();
  }

  @Test
  public void testBeforeAgent() {
    DeclarativeConfigProperties configProperties =
        DeclarativeConfiguration.toConfigProperties(solarwinds);
    when(autoConfiguredOpenTelemetrySdkMock.getOpenTelemetrySdk())
        .thenReturn(extendedOpenTelemetrySdkMock);
    when(extendedOpenTelemetrySdkMock.getInstrumentationConfig(eq("solarwinds")))
        .thenReturn(configProperties);
    tested.beforeAgent(autoConfiguredOpenTelemetrySdkMock);

    assertNotNull(ConfigManager.getConfig(ConfigProperty.AGENT_COLLECTOR));
    assertNotNull(ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY));
  }
}
