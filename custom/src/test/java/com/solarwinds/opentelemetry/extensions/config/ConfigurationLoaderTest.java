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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import org.junit.jupiter.api.Assertions;
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

    Assertions.assertEquals(expected, TransactionNameManager.getNamingScheme());
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

  @Test
  @ClearSystemProperty(key = "otel.logs.exporter")
  @ClearSystemProperty(key = "otel.exporter.otlp.protocol")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.headers")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.endpoint")
  void verifySettingOtelLogExportSystemVariablesWhenEnabled() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    configContainer.putByStringValue(ConfigProperty.AGENT_SERVICE_KEY, "token:service");
    configContainer.putByStringValue(
        ConfigProperty.AGENT_COLLECTOR, "apm.collector.na-02.cloud.solarwinds.com");

    configContainer.putByStringValue(ConfigProperty.AGENT_EXPORT_LOGS_ENABLED, "true");
    ConfigurationLoader.configureOtelLogExport(configContainer);

    assertEquals("otlp", System.getProperty("otel.logs.exporter"));
    assertEquals("grpc", System.getProperty("otel.exporter.otlp.logs.protocol"));

    assertEquals(
        "https://otel.collector.na-02.cloud.solarwinds.com",
        System.getProperty("otel.exporter.otlp.logs.endpoint"));
    assertEquals(
        "authorization=Bearer token", System.getProperty("otel.exporter.otlp.logs.headers"));
  }

  @Test
  @ClearSystemProperty(key = "otel.logs.exporter")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.protocol")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.headers")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.endpoint")
  void verifyOtelLogExportSystemVariablesAreNotSetWhenNotEnabled() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    configContainer.putByStringValue(ConfigProperty.AGENT_SERVICE_KEY, "token:service");
    configContainer.putByStringValue(
        ConfigProperty.AGENT_COLLECTOR, "apm.collector.na-02.cloud.solarwinds.com");

    ConfigurationLoader.configureOtelLogExport(configContainer);

    assertNull(System.getProperty("otel.logs.exporter"));
    assertNull(System.getProperty("otel.exporter.otlp.logs.protocol"));

    assertNull(System.getProperty("otel.exporter.otlp.logs.endpoint"));
    assertNull(System.getProperty("otel.exporter.otlp.logs.headers"));
  }

  @Test
  @ClearSystemProperty(key = "otel.logs.exporter")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.protocol")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.headers")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.endpoint")
  void verifyOtelLogExportEndpointIsProperlyFormed() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    configContainer.putByStringValue(ConfigProperty.AGENT_SERVICE_KEY, "token:service");
    configContainer.putByStringValue(
        ConfigProperty.AGENT_COLLECTOR, "apm.collector.na-02.staging.solarwinds.com");

    configContainer.putByStringValue(ConfigProperty.AGENT_EXPORT_LOGS_ENABLED, "true");
    ConfigurationLoader.configureOtelLogExport(configContainer);

    assertEquals(
        "https://otel.collector.na-02.staging.solarwinds.com",
        System.getProperty("otel.exporter.otlp.logs.endpoint"));
  }

  @Test
  @ClearSystemProperty(key = "otel.logs.exporter")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.protocol")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.headers")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.endpoint")
  void verifyOtelLogExportEndpointIsProperlyFormedWithPort() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    configContainer.putByStringValue(ConfigProperty.AGENT_SERVICE_KEY, "token:service");
    configContainer.putByStringValue(
        ConfigProperty.AGENT_COLLECTOR, "otel.collector.na-01.cloud.solarwinds.com:443");

    configContainer.putByStringValue(ConfigProperty.AGENT_EXPORT_LOGS_ENABLED, "true");
    ConfigurationLoader.configureOtelLogExport(configContainer);

    assertEquals(
        "https://otel.collector.na-01.cloud.solarwinds.com",
        System.getProperty("otel.exporter.otlp.logs.endpoint"));
  }

  @Test
  @ClearSystemProperty(key = "otel.logs.exporter")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.protocol")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.headers")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.endpoint")
  void verifyConfigureOtelLogExportThrowsForAO() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    configContainer.putByStringValue(ConfigProperty.AGENT_SERVICE_KEY, "token:service");
    configContainer.putByStringValue(ConfigProperty.AGENT_COLLECTOR, "collector.appoptics.com:443");

    configContainer.putByStringValue(ConfigProperty.AGENT_EXPORT_LOGS_ENABLED, "true");
    assertThrows(
        InvalidConfigException.class,
        () -> ConfigurationLoader.configureOtelLogExport(configContainer));
  }

  @Test
  @ClearSystemProperty(key = "otel.logs.exporter")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.protocol")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.headers")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.endpoint")
  void verifyConfigureOtelTraceExportThrowsForAO() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    configContainer.putByStringValue(ConfigProperty.AGENT_SERVICE_KEY, "token:service");
    configContainer.putByStringValue(ConfigProperty.AGENT_COLLECTOR, "collector.appoptics.com:443");

    assertThrows(
        InvalidConfigException.class,
        () -> ConfigurationLoader.configureOtelTraceExport(configContainer));
  }

  @Test
  @ClearSystemProperty(key = "otel.logs.exporter")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.protocol")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.headers")
  @ClearSystemProperty(key = "otel.exporter.otlp.logs.endpoint")
  void verifyDefaultEndpointIsUsed() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    configContainer.putByStringValue(ConfigProperty.AGENT_SERVICE_KEY, "token:service");

    configContainer.putByStringValue(ConfigProperty.AGENT_EXPORT_LOGS_ENABLED, "true");
    ConfigurationLoader.configureOtelLogExport(configContainer);

    assertEquals(
        "https://otel.collector.na-01.cloud.solarwinds.com",
        System.getProperty("otel.exporter.otlp.logs.endpoint"));
  }

  @Test
  @ClearSystemProperty(key = "otel.metrics.exporter")
  @ClearSystemProperty(key = "otel.exporter.otlp.metrics.protocol")
  @ClearSystemProperty(key = "otel.exporter.otlp.metrics.headers")
  @ClearSystemProperty(key = "otel.exporter.otlp.metrics.endpoint")
  void verifySettingOtelMetricExportSystemVariablesWhenEnabled() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    configContainer.putByStringValue(ConfigProperty.AGENT_SERVICE_KEY, "token:service");
    configContainer.putByStringValue(
        ConfigProperty.AGENT_COLLECTOR, "apm.collector.na-02.cloud.solarwinds.com");

    configContainer.putByStringValue(ConfigProperty.AGENT_EXPORT_METRICS_ENABLED, "true");
    ConfigurationLoader.configureOtelMetricExport(configContainer);

    assertEquals("otlp", System.getProperty("otel.metrics.exporter"));
    assertEquals("grpc", System.getProperty("otel.exporter.otlp.metrics.protocol"));

    assertEquals(
        "https://otel.collector.na-02.cloud.solarwinds.com",
        System.getProperty("otel.exporter.otlp.metrics.endpoint"));
    assertEquals(
        "authorization=Bearer token", System.getProperty("otel.exporter.otlp.metrics.headers"));
  }

  @Test
  @ClearSystemProperty(key = "otel.metrics.exporter")
  @ClearSystemProperty(key = "otel.exporter.otlp.metrics.protocol")
  @ClearSystemProperty(key = "otel.exporter.otlp.metrics.headers")
  @ClearSystemProperty(key = "otel.exporter.otlp.metrics.endpoint")
  void verifyOtelMetricExportSystemVariablesAreSetByDefault() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    configContainer.putByStringValue(ConfigProperty.AGENT_SERVICE_KEY, "token:service");
    configContainer.putByStringValue(
        ConfigProperty.AGENT_COLLECTOR, "apm.collector.na-02.cloud.solarwinds.com");

    ConfigurationLoader.configureOtelMetricExport(configContainer);

    assertNotNull(System.getProperty("otel.metrics.exporter"));
    assertNotNull(System.getProperty("otel.exporter.otlp.metrics.protocol"));

    assertNotNull(System.getProperty("otel.exporter.otlp.metrics.endpoint"));
    assertNotNull(System.getProperty("otel.exporter.otlp.metrics.headers"));
  }

  @Test
  @ClearSystemProperty(key = "otel.metrics.exporter")
  @ClearSystemProperty(key = "otel.exporter.otlp.metrics.protocol")
  @ClearSystemProperty(key = "otel.exporter.otlp.metrics.headers")
  @ClearSystemProperty(key = "otel.exporter.otlp.metrics.endpoint")
  void verifyOtelMetricExportEndpointIsProperlyFormed() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    configContainer.putByStringValue(ConfigProperty.AGENT_SERVICE_KEY, "token:service");
    configContainer.putByStringValue(
        ConfigProperty.AGENT_COLLECTOR, "apm.collector.na-02.staging.solarwinds.com");

    configContainer.putByStringValue(ConfigProperty.AGENT_EXPORT_METRICS_ENABLED, "true");
    ConfigurationLoader.configureOtelMetricExport(configContainer);

    assertEquals(
        "https://otel.collector.na-02.staging.solarwinds.com",
        System.getProperty("otel.exporter.otlp.metrics.endpoint"));
  }

  @Test
  @ClearSystemProperty(key = "otel.metrics.exporter")
  @ClearSystemProperty(key = "otel.exporter.otlp.metrics.protocol")
  @ClearSystemProperty(key = "otel.exporter.otlp.metrics.headers")
  @ClearSystemProperty(key = "otel.exporter.otlp.metrics.endpoint")
  void verifyOtelMetricExportEndpointIsProperlyFormedWithPort() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    configContainer.putByStringValue(ConfigProperty.AGENT_SERVICE_KEY, "token:service");
    configContainer.putByStringValue(
        ConfigProperty.AGENT_COLLECTOR, "otel.collector.na-01.cloud.solarwinds.com:443");

    configContainer.putByStringValue(ConfigProperty.AGENT_EXPORT_METRICS_ENABLED, "true");
    ConfigurationLoader.configureOtelMetricExport(configContainer);

    assertEquals(
        "https://otel.collector.na-01.cloud.solarwinds.com",
        System.getProperty("otel.exporter.otlp.metrics.endpoint"));
  }

  @Test
  @ClearSystemProperty(key = "otel.metrics.exporter")
  @ClearSystemProperty(key = "otel.exporter.otlp.metrics.protocol")
  @ClearSystemProperty(key = "otel.exporter.otlp.metrics.headers")
  @ClearSystemProperty(key = "otel.exporter.otlp.metrics.endpoint")
  void verifyConfigureOtelMetricExportThrowsForAO() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    configContainer.putByStringValue(ConfigProperty.AGENT_SERVICE_KEY, "token:service");
    configContainer.putByStringValue(ConfigProperty.AGENT_COLLECTOR, "collector.appoptics.com:443");

    configContainer.putByStringValue(ConfigProperty.AGENT_EXPORT_METRICS_ENABLED, "true");
    assertThrows(
        InvalidConfigException.class,
        () -> ConfigurationLoader.configureOtelMetricExport(configContainer));
  }

  @Test
  @ClearSystemProperty(key = "otel.metrics.exporter")
  @ClearSystemProperty(key = "otel.exporter.otlp.metrics.protocol")
  @ClearSystemProperty(key = "otel.exporter.otlp.metrics.headers")
  @ClearSystemProperty(key = "otel.exporter.otlp.metrics.endpoint")
  void verifyDefaultEndpointIsUsedForMetric() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    configContainer.putByStringValue(ConfigProperty.AGENT_SERVICE_KEY, "token:service");

    configContainer.putByStringValue(ConfigProperty.AGENT_EXPORT_METRICS_ENABLED, "true");
    ConfigurationLoader.configureOtelMetricExport(configContainer);

    assertEquals(
        "https://otel.collector.na-01.cloud.solarwinds.com",
        System.getProperty("otel.exporter.otlp.metrics.endpoint"));
  }

  @Test
  void returnEnvironmentVariableEquivalent() {
    assertEquals(
        "OTEL_EXPORTER_OTLP_ENDPOINT",
        ConfigurationLoader.normalize("otel.exporter.otlp.endpoint"));
  }
}
