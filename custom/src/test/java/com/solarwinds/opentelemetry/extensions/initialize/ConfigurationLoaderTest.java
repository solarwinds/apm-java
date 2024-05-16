/*
 * Copyright SolarWinds Worldwide, LLC.
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

package com.solarwinds.opentelemetry.extensions.initialize;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.solarwinds.joboe.config.ConfigContainer;
import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.config.ServiceKeyUtils;
import com.solarwinds.opentelemetry.extensions.DefaultNamingScheme;
import com.solarwinds.opentelemetry.extensions.NamingScheme;
import com.solarwinds.opentelemetry.extensions.SpanAttributeNamingScheme;
import com.solarwinds.opentelemetry.extensions.TransactionNameManager;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigurationLoaderTest {

  @InjectMocks private ConfigurationLoader tested;

  @Test
  void processConfigs() throws InvalidConfigException {
    String json =
        "  [\n"
            + "   {\n"
            + "      \"scheme\": \"spanAttribute\",\n"
            + "      \"delimiter\": \"-\",\n"
            + "      \"attributes\": [\"http.method\"]\n"
            + "    },\n"
            + "    {\n"
            + "      \"scheme\": \"spanAttribute\",\n"
            + "      \"delimiter\": \":\",\n"
            + "      \"attributes\": [\"http.route\",\"HandlerName\"]\n"
            + "    }\n"
            + "  ]\n";

    ConfigContainer configContainer = new ConfigContainer();
    configContainer.putByStringValue(ConfigProperty.AGENT_TRANSACTION_NAMING_SCHEMES, json);
    configContainer.putByStringValue(ConfigProperty.AGENT_SERVICE_KEY, "Key");

    ConfigurationLoader.processConfigs(configContainer);
    NamingScheme expected =
        new SpanAttributeNamingScheme(
            new SpanAttributeNamingScheme(
                new DefaultNamingScheme(null), ":", Arrays.asList("http.route", "HandlerName")),
            "-",
            Collections.singletonList("http.method"));

    assertEquals(expected, TransactionNameManager.getNamingScheme());
  }

  @Test
  void testUnixPath() {
    String path = "/usr/config.json";
    assertDoesNotThrow(() -> ConfigurationLoader.setWatchedPaths(path, '/'));

    assertNotNull(ConfigurationLoader.getConfigurationFileDir());
    assertNotNull(ConfigurationLoader.getRuntimeConfigFilename());

    assertEquals("/usr", ConfigurationLoader.getConfigurationFileDir());
    assertEquals("config.json", ConfigurationLoader.getRuntimeConfigFilename());
  }

  @Test
  void testWindowsPath() {
    String path = "C:\\Program Files\\SolarWinds\\APM\\java\\config.json";
    assertDoesNotThrow(() -> ConfigurationLoader.setWatchedPaths(path, '\\'));

    assertNotNull(ConfigurationLoader.getConfigurationFileDir());
    assertNotNull(ConfigurationLoader.getRuntimeConfigFilename());

    assertEquals(
        "C:\\Program Files\\SolarWinds\\APM\\java", ConfigurationLoader.getConfigurationFileDir());
    assertEquals("config.json", ConfigurationLoader.getRuntimeConfigFilename());
  }

  @Test
  void testFilenameAsPath() {
    String path = "config.json";
    ConfigurationLoader.resetWatchedPaths();
    assertDoesNotThrow(() -> ConfigurationLoader.setWatchedPaths(path, File.separatorChar));

    assertNull(ConfigurationLoader.getConfigurationFileDir());
    assertNull(ConfigurationLoader.getRuntimeConfigFilename());
  }

  @Test
  void testUnixRootPath() {
    String path = "/";
    ConfigurationLoader.resetWatchedPaths();
    assertDoesNotThrow(() -> ConfigurationLoader.setWatchedPaths(path, File.separatorChar));

    assertNull(ConfigurationLoader.getConfigurationFileDir());
    assertNull(ConfigurationLoader.getRuntimeConfigFilename());
  }

  @Test
  void testWindowsRootPath() {
    String path = "C:";
    ConfigurationLoader.resetWatchedPaths();
    assertDoesNotThrow(() -> ConfigurationLoader.setWatchedPaths(path, File.separatorChar));

    assertNull(ConfigurationLoader.getConfigurationFileDir());
    assertNull(ConfigurationLoader.getRuntimeConfigFilename());
  }

  @Test
  @ClearSystemProperty(key = "otel.service.name")
  void verifyThatOtelServiceNameSystemPropertyIsWhenNotExplicitlySet()
      throws InvalidConfigException {
    ConfigurationLoader.load();
    assertEquals(
        "name" /*name is from custom/build.gradle test task*/,
        System.getProperty("otel.service.name"));
  }

  @Test
  @SetSystemProperty(key = "otel.service.name", value = "test")
  void verifyThatServiceKeyIsUpdatedWithOtelServiceNameWhenSystemPropertyIsSet()
      throws InvalidConfigException {
    ConfigurationLoader.load();
    String serviceKeyAfter = (String) ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY);

    assertEquals("test", ServiceKeyUtils.getServiceName(serviceKeyAfter));
    assertEquals("token:test", serviceKeyAfter);
  }
}
