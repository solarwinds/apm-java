package com.solarwinds.opentelemetry.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.solarwinds.joboe.config.ConfigContainer;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LambdaConfigurationLoaderTest {
  @InjectMocks private LambdaConfigurationLoader tested;

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

    LambdaConfigurationLoader.processConfigs(configContainer);
    NamingScheme expected =
        new SpanAttributeNamingScheme(
            new SpanAttributeNamingScheme(
                new DefaultNamingScheme(null), ":", Arrays.asList("http.route", "HandlerName")),
            "-",
            Collections.singletonList("http.method"));

    assertEquals(expected, TransactionNameManager.getNamingScheme());
  }
}
