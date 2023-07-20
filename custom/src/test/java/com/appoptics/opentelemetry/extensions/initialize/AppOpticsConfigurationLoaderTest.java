package com.appoptics.opentelemetry.extensions.initialize;

import com.appoptics.opentelemetry.extensions.TransactionNameManager;
import com.appoptics.opentelemetry.extensions.attrrename.AttributeRenamer;
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

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class AppOpticsConfigurationLoaderTest {

    @InjectMocks
    private AppOpticsConfigurationLoader tested;

    @Test
    void processTransactionSchemesConfig() throws InvalidConfigException {
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
    void processAttributeRenameConfig() throws InvalidConfigException {
        String attrRename = "old_key_0=new_key_0,old_key_1= new_key_1";
        ConfigContainer configContainer = new ConfigContainer();
        configContainer.putByStringValue(ConfigProperty.AGENT_ATTR_RENAME, attrRename);

        configContainer.putByStringValue(ConfigProperty.AGENT_SERVICE_KEY, "Key");
        AppOpticsConfigurationLoader.processConfigs(configContainer);
        String actual = AttributeRenamer.getInstance().rename("old_key_0");

        String expected = "new_key_0";
        assertEquals(expected, actual);
    }
}