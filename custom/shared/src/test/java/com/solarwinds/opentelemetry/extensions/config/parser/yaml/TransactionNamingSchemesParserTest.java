package com.solarwinds.opentelemetry.extensions.config.parser.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.opentelemetry.extensions.TransactionNamingScheme;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TransactionNamingSchemesParserTest {
  private static DeclarativeConfigProperties declarativeConfigProperties;
  private final TransactionNamingSchemesParser tested = new TransactionNamingSchemesParser();

  @BeforeAll
  static void setup() {
    try (InputStream resourceAsStream =
        TransactionNamingSchemesParserTest.class.getResourceAsStream("/sdk-config.yaml")) {
      DeclarativeConfigProperties configProperties =
          DeclarativeConfiguration.toConfigProperties(resourceAsStream);
      declarativeConfigProperties =
          configProperties
              .getStructured("instrumentation", DeclarativeConfigProperties.empty())
              .getStructured("java", DeclarativeConfigProperties.empty())
              .getStructured("solarwinds");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testConvert() throws InvalidConfigException {
    List<TransactionNamingScheme> transactionNamingSchemes =
        tested.convert(declarativeConfigProperties);
    assertEquals(
        Collections.singletonList(
            new TransactionNamingScheme(
                "spanAttribute", ":", Collections.singletonList("HandlerName"))),
        transactionNamingSchemes);
  }

  @Test
  void configKey() {
    assertEquals("agent.transactionNameSchemes", tested.configKey());
  }
}
