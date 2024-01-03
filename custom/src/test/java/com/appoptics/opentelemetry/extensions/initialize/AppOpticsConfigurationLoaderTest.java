package com.appoptics.opentelemetry.extensions.initialize;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.appoptics.opentelemetry.extensions.TransactionNameManager;
import com.appoptics.opentelemetry.extensions.transaction.DefaultNamingScheme;
import com.appoptics.opentelemetry.extensions.transaction.NamingScheme;
import com.appoptics.opentelemetry.extensions.transaction.SpanAttributeNamingScheme;
import com.solarwinds.joboe.core.config.ConfigContainer;
import com.solarwinds.joboe.core.config.ConfigManager;
import com.solarwinds.joboe.core.config.ConfigProperty;
import com.solarwinds.joboe.core.config.InvalidConfigException;
import com.solarwinds.joboe.core.util.ServiceKeyUtils;
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
class AppOpticsConfigurationLoaderTest {

  @InjectMocks private AppOpticsConfigurationLoader tested;

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

    AppOpticsConfigurationLoader.processConfigs(configContainer);
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
    assertDoesNotThrow(() -> AppOpticsConfigurationLoader.setWatchedPaths(path, '/'));

    assertNotNull(AppOpticsConfigurationLoader.getConfigurationFileDir());
    assertNotNull(AppOpticsConfigurationLoader.getRuntimeConfigFilename());

    assertEquals("/usr", AppOpticsConfigurationLoader.getConfigurationFileDir());
    assertEquals("config.json", AppOpticsConfigurationLoader.getRuntimeConfigFilename());
  }

  @Test
  void testWindowsPath() {
    String path = "C:\\Program Files\\SolarWinds\\APM\\java\\config.json";
    assertDoesNotThrow(() -> AppOpticsConfigurationLoader.setWatchedPaths(path, '\\'));

    assertNotNull(AppOpticsConfigurationLoader.getConfigurationFileDir());
    assertNotNull(AppOpticsConfigurationLoader.getRuntimeConfigFilename());

    assertEquals(
        "C:\\Program Files\\SolarWinds\\APM\\java",
        AppOpticsConfigurationLoader.getConfigurationFileDir());
    assertEquals("config.json", AppOpticsConfigurationLoader.getRuntimeConfigFilename());
  }

  @Test
  void testFilenameAsPath() {
    String path = "config.json";
    AppOpticsConfigurationLoader.resetWatchedPaths();
    assertDoesNotThrow(
        () -> AppOpticsConfigurationLoader.setWatchedPaths(path, File.separatorChar));

    assertNull(AppOpticsConfigurationLoader.getConfigurationFileDir());
    assertNull(AppOpticsConfigurationLoader.getRuntimeConfigFilename());
  }

  @Test
  void testUnixRootPath() {
    String path = "/";
    AppOpticsConfigurationLoader.resetWatchedPaths();
    assertDoesNotThrow(
        () -> AppOpticsConfigurationLoader.setWatchedPaths(path, File.separatorChar));

    assertNull(AppOpticsConfigurationLoader.getConfigurationFileDir());
    assertNull(AppOpticsConfigurationLoader.getRuntimeConfigFilename());
  }

  @Test
  void testWindowsRootPath() {
    String path = "C:";
    AppOpticsConfigurationLoader.resetWatchedPaths();
    assertDoesNotThrow(
        () -> AppOpticsConfigurationLoader.setWatchedPaths(path, File.separatorChar));

    assertNull(AppOpticsConfigurationLoader.getConfigurationFileDir());
    assertNull(AppOpticsConfigurationLoader.getRuntimeConfigFilename());
  }

  @Test
  @ClearSystemProperty(key = "otel.service.name")
  void verifyThatOtelServiceNameSystemPropertyIsWhenNotExplicitlySet()
      throws InvalidConfigException {
    AppOpticsConfigurationLoader.load();
    assertEquals(
        "name" /*name is from custom/build.gradle test task*/,
        System.getProperty("otel.service.name"));
  }

  @Test
  @SetSystemProperty(key = "otel.service.name", value = "test")
  void verifyThatServiceKeyIsUpdatedWithOtelServiceNameWhenSystemPropertyIsSet()
      throws InvalidConfigException {
    AppOpticsConfigurationLoader.load();
    String serviceKeyAfter = (String) ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY);

    assertEquals("test", ServiceKeyUtils.getServiceName(serviceKeyAfter));
    assertEquals("token:test", serviceKeyAfter);
  }
}
