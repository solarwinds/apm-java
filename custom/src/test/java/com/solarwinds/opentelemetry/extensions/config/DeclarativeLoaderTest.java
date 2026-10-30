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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.logging.LogSetting;
import com.solarwinds.joboe.logging.Logger;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.DistributionModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ExperimentalInstrumentationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ExperimentalLanguageSpecificInstrumentationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ExperimentalLanguageSpecificInstrumentationPropertyModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.OpenTelemetryConfigurationModelAccessor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    OpenTelemetryConfigurationModel model = model(distribution("debug"), null);
    OpenTelemetryConfigurationModel returned = customize(model);

    assertEquals(Logger.Level.DEBUG, loggingLevel());
    assertEquals(model, returned);
  }

  @Test
  public void testCustomizeIgnoresInstrumentationConfig() {
    OpenTelemetryConfigurationModel model = model(distribution("debug"), instrumentation("trace"));

    customize(model);

    assertEquals(Logger.Level.DEBUG, loggingLevel());
  }

  @Test
  public void testCustomizeWithEmptyModelDoesNotThrows() {
    OpenTelemetryConfigurationModel model = new OpenTelemetryConfigurationModel();

    assertDoesNotThrow(() -> customize(model));
    assertNull(ConfigManager.getConfig(ConfigProperty.AGENT_LOGGING));
  }

  @Test
  public void testCustomizeThrowsOnUnknownKey() {
    OpenTelemetryConfigurationModel model =
        model(
            new DistributionModel()
                .setExtensionProperty(
                    "solarwinds", Collections.singletonMap("not_a_real_config_key", "value")),
            null);

    assertThrows(RuntimeException.class, () -> customize(model));
  }

  @Test
  public void testCustomizeThrowsOnServiceKey() {
    OpenTelemetryConfigurationModel model =
        model(
            new DistributionModel()
                .setExtensionProperty(
                    "solarwinds",
                    Collections.singletonMap(
                        ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(), "token:service")),
            null);

    assertThrows(RuntimeException.class, () -> customize(model));
    assertNull(ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY));
  }

  private static Logger.Level loggingLevel() {
    return ((LogSetting) ConfigManager.getConfig(ConfigProperty.AGENT_LOGGING)).getLevel();
  }

  private static OpenTelemetryConfigurationModel model(
      DistributionModel distribution, ExperimentalInstrumentationModel instrumentation) {
    OpenTelemetryConfigurationModel model = new OpenTelemetryConfigurationModel();
    if (distribution != null) {
      model.setDistribution(distribution);
    }

    if (instrumentation != null) {
      OpenTelemetryConfigurationModelAccessor.setInstrumentation(model, instrumentation);
    }
    return model;
  }

  private static Map<String, Object> logging(String level) {
    return Collections.singletonMap("level", level);
  }

  private static DistributionModel distribution(String loggingLevel) {
    Map<String, Object> properties = new HashMap<>();
    properties.put("logging", logging(loggingLevel));
    return new DistributionModel().setExtensionProperty("solarwinds", properties);
  }

  private static ExperimentalInstrumentationModel instrumentation(String loggingLevel) {
    return new ExperimentalInstrumentationModel()
        .setJava(
            new ExperimentalLanguageSpecificInstrumentationModel()
                .setAdditionalProperty(
                    "solarwinds",
                    new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                        .setAdditionalProperty("logging", logging(loggingLevel))));
  }

  private OpenTelemetryConfigurationModel customize(OpenTelemetryConfigurationModel model) {
    tested.customize(customizerMock);
    verify(customizerMock).addModelCustomizer(modelCustomizerCaptor.capture());
    return modelCustomizerCaptor.getValue().apply(model);
  }
}
