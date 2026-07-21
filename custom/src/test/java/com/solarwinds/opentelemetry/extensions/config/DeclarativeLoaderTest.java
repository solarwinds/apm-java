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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.DistributionModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.DistributionPropertyModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ExperimentalInstrumentationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ExperimentalLanguageSpecificInstrumentationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ExperimentalLanguageSpecificInstrumentationPropertyModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import java.util.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeclarativeLoaderTest {

  @InjectMocks private DeclarativeLoader tested;

  @Mock private DeclarativeConfigurationCustomizer customizerMock;

  @Captor
  private ArgumentCaptor<Function<OpenTelemetryConfigurationModel, OpenTelemetryConfigurationModel>>
      modelCustomizerCaptor;

  @BeforeEach
  public void resetConfig() {
    ConfigManager.reset();
  }

  @AfterAll
  public static void clearConfig() {
    ConfigManager.reset();
  }

  @Test
  public void testCustomizeLoadsDistributionConfig() {
    OpenTelemetryConfigurationModel model =
        new OpenTelemetryConfigurationModel()
            .withDistribution(
                new DistributionModel()
                    .withAdditionalProperty(
                        "solarwinds",
                        new DistributionPropertyModel()
                            .withAdditionalProperty(
                                ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                "token:service")
                            .withAdditionalProperty(
                                ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                "apm.collector.com")));

    OpenTelemetryConfigurationModel returned = customize(model);

    assertNotNull(ConfigManager.getConfig(ConfigProperty.AGENT_COLLECTOR));
    assertNotNull(ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY));
    assertEquals(model, returned);
  }

  @Test
  public void testCustomizeLoadsInstrumentationConfig() {
    OpenTelemetryConfigurationModel model =
        new OpenTelemetryConfigurationModel()
            .withInstrumentationDevelopment(
                new ExperimentalInstrumentationModel()
                    .withJava(
                        new ExperimentalLanguageSpecificInstrumentationModel()
                            .withAdditionalProperty(
                                "solarwinds",
                                new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                    .withAdditionalProperty(
                                        ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                        "token:service")
                                    .withAdditionalProperty(
                                        ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                        "apm.collector.com"))));

    customize(model);

    assertNotNull(ConfigManager.getConfig(ConfigProperty.AGENT_COLLECTOR));
    assertNotNull(ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY));
  }

  @Test
  public void testDistributionTakesPrecedenceWhenBothNodesPresent() {
    OpenTelemetryConfigurationModel model =
        new OpenTelemetryConfigurationModel()
            .withDistribution(distribution("distribution-token:distribution-service"))
            .withInstrumentationDevelopment(instrumentation("instrumentation-token:inst-service"));

    customize(model);

    assertEquals(
        "distribution-token:distribution-service",
        ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY));
  }

  @Test
  public void testCustomizeWithEmptyModelThrows() {
    // No source node is present, so both are skipped and processConfigs runs against an empty
    // container, which fails because the service key is required.
    OpenTelemetryConfigurationModel model = new OpenTelemetryConfigurationModel();

    assertThrows(RuntimeException.class, () -> customize(model));
    assertNull(ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY));
  }

  @Test
  public void testCustomizeSkipsEmptySolarwindsNode() {
    // distribution.solarwinds is present but has no properties, so it is skipped; configuration is
    // still loaded from the instrumentation node.
    OpenTelemetryConfigurationModel model =
        new OpenTelemetryConfigurationModel()
            .withDistribution(
                new DistributionModel()
                    .withAdditionalProperty("solarwinds", new DistributionPropertyModel()))
            .withInstrumentationDevelopment(instrumentation("token:service"));

    customize(model);

    assertEquals("token:service", ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY));
  }

  @Test
  public void testCustomizeThrowsOnUnknownKey() {
    OpenTelemetryConfigurationModel model =
        new OpenTelemetryConfigurationModel()
            .withDistribution(
                new DistributionModel()
                    .withAdditionalProperty(
                        "solarwinds",
                        new DistributionPropertyModel()
                            .withAdditionalProperty("not_a_real_config_key", "value")));

    assertThrows(RuntimeException.class, () -> customize(model));
  }

  @Test
  public void testCustomizeThrowsWhenServiceKeyMissing() {
    OpenTelemetryConfigurationModel model =
        new OpenTelemetryConfigurationModel()
            .withDistribution(
                new DistributionModel()
                    .withAdditionalProperty(
                        "solarwinds",
                        new DistributionPropertyModel()
                            .withAdditionalProperty(
                                ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                "apm.collector.com")));

    assertThrows(RuntimeException.class, () -> customize(model));
  }

  private static DistributionModel distribution(String serviceKey) {
    return new DistributionModel()
        .withAdditionalProperty(
            "solarwinds",
            new DistributionPropertyModel()
                .withAdditionalProperty(
                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(), serviceKey)
                .withAdditionalProperty(
                    ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(), "apm.collector.com"));
  }

  private static ExperimentalInstrumentationModel instrumentation(String serviceKey) {
    return new ExperimentalInstrumentationModel()
        .withJava(
            new ExperimentalLanguageSpecificInstrumentationModel()
                .withAdditionalProperty(
                    "solarwinds",
                    new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                        .withAdditionalProperty(
                            ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(), serviceKey)
                        .withAdditionalProperty(
                            ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                            "apm.collector.com")));
  }

  private OpenTelemetryConfigurationModel customize(OpenTelemetryConfigurationModel model) {
    tested.customize(customizerMock);
    verify(customizerMock).addModelCustomizer(modelCustomizerCaptor.capture());
    return modelCustomizerCaptor.getValue().apply(model);
  }
}
