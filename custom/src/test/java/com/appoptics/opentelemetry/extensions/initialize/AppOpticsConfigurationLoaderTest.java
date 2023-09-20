package com.appoptics.opentelemetry.extensions.initialize;

import com.appoptics.opentelemetry.extensions.TransactionNameManager;
import com.appoptics.opentelemetry.extensions.transaction.DefaultNamingScheme;
import com.appoptics.opentelemetry.extensions.transaction.NamingScheme;
import com.appoptics.opentelemetry.extensions.transaction.SpanAttributeNamingScheme;
import com.tracelytics.joboe.config.ConfigContainer;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.InvalidConfigException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AppOpticsConfigurationLoaderTest {

    @InjectMocks
    private AppOpticsConfigurationLoader tested;

    @Test
    void processConfigs() throws InvalidConfigException {
        String json =
                "  [\n" +
                        "   {\n" +
                        "      \"scheme\": \"spanAttribute\",\n" +
                        "      \"delimiter\": \"-\",\n" +
                        "      \"attributes\": [\"http.method\"]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"scheme\": \"spanAttribute\",\n" +
                        "      \"delimiter\": \":\",\n" +
                        "      \"attributes\": [\"http.route\",\"HandlerName\"]\n" +
                        "    }\n" +
                        "  ]\n";

        ConfigContainer configContainer = new ConfigContainer();
        configContainer.putByStringValue(ConfigProperty.AGENT_TRANSACTION_NAMING_SCHEMES, json);
        configContainer.putByStringValue(ConfigProperty.AGENT_SERVICE_KEY, "Key");

        AppOpticsConfigurationLoader.processConfigs(configContainer);
        NamingScheme expected = new SpanAttributeNamingScheme(
                new SpanAttributeNamingScheme(new DefaultNamingScheme(null),
                        ":", Arrays.asList("http.route", "HandlerName")),
                "-", Collections.singletonList("http.method"));

        assertEquals(expected, TransactionNameManager.getNamingScheme());
    }

    @Test
    void testUnixPath() {
        String path = "/usr/config.json";
        assertDoesNotThrow(() -> AppOpticsConfigurationLoader.setWatchedPaths(path, '/'));

        assertNotNull(AppOpticsConfigurationLoader.getConfigurationFileDir());
        assertNotNull(AppOpticsConfigurationLoader.getRuntimeConfigFilename());

        assertEquals( "/usr",AppOpticsConfigurationLoader.getConfigurationFileDir());
        assertEquals("config.json", AppOpticsConfigurationLoader.getRuntimeConfigFilename());
    }

    @Test
    void testWindowsPath() {
        String path = "C:\\Program Files\\SolarWinds\\APM\\java\\config.json";
        assertDoesNotThrow(() -> AppOpticsConfigurationLoader.setWatchedPaths(path, '\\'));

        assertNotNull(AppOpticsConfigurationLoader.getConfigurationFileDir());
        assertNotNull(AppOpticsConfigurationLoader.getRuntimeConfigFilename());

        assertEquals( "C:\\Program Files\\SolarWinds\\APM\\java",AppOpticsConfigurationLoader.getConfigurationFileDir());
        assertEquals("config.json", AppOpticsConfigurationLoader.getRuntimeConfigFilename());
    }

    @Test
    void testFilenameAsPath() {
        String path = "config.json";
        AppOpticsConfigurationLoader.resetWatchedPaths();
        assertDoesNotThrow(() -> AppOpticsConfigurationLoader.setWatchedPaths(path, File.separatorChar));

        assertNull(AppOpticsConfigurationLoader.getConfigurationFileDir());
        assertNull(AppOpticsConfigurationLoader.getRuntimeConfigFilename());
    }

    @Test
    void testUnixRootPath() {
        String path = "/";
        AppOpticsConfigurationLoader.resetWatchedPaths();
        assertDoesNotThrow(() -> AppOpticsConfigurationLoader.setWatchedPaths(path, File.separatorChar));

        assertNull(AppOpticsConfigurationLoader.getConfigurationFileDir());
        assertNull(AppOpticsConfigurationLoader.getRuntimeConfigFilename());
    }

    @Test
    void testWindowsRootPath() {
        String path = "C:";
        AppOpticsConfigurationLoader.resetWatchedPaths();
        assertDoesNotThrow(() -> AppOpticsConfigurationLoader.setWatchedPaths(path, File.separatorChar));

        assertNull(AppOpticsConfigurationLoader.getConfigurationFileDir());
        assertNull(AppOpticsConfigurationLoader.getRuntimeConfigFilename());
    }
}