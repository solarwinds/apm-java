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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.sampling.SettingsManager;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class JsonSamplerComponentProviderTest {
  private final JsonSamplerComponentProvider tested = new JsonSamplerComponentProvider();

  @AfterEach
  void tearDown() {
    ConfigManager.reset();
    SettingsManager.reset();
  }

  @Test
  void testName() {
    assertEquals("swo_json", tested.getName());
  }

  @Test
  void testCreateReadsSettingsFromConfiguredFile() {
    String file =
        JsonSamplerComponentProviderTest.class
            .getResource("/solarwinds-apm-settings.json")
            .getPath();

    assertInstanceOf(
        SolarwindsSampler.class, tested.create(fromYaml("swo_json:\n  file: " + file + "\n")));
    assertNotNull(SettingsManager.getSettings());
  }

  @Test
  void testCreateDoesNotFallBackToDefaultFile() {
    tested.create(fromYaml("swo_json:\n  file: /does/not/exist.json\n"));
    assertNull(SettingsManager.getSettings());
  }

  @Test
  void testCreateThrowsForEmptyFile() {
    RuntimeException exception =
        assertThrows(
            RuntimeException.class, () -> tested.create(fromYaml("swo_json:\n  file: ' '\n")));
    assertInstanceOf(InvalidConfigException.class, exception.getCause());
  }

  private DeclarativeConfigProperties fromYaml(String yaml) {
    return DeclarativeConfiguration.toConfigProperties(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
        .getStructured("swo_json");
  }
}
