package com.solarwinds.opentelemetry.extensions.config.parser.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.solarwinds.joboe.config.InvalidConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SqlTagDatabasesParserTest {
  private static DeclarativeConfigProperties declarativeConfigProperties;
  private final SqlTagDatabasesParser tested = new SqlTagDatabasesParser();

  @BeforeAll
  static void setup() {
    try (InputStream resourceAsStream =
        SqlTagDatabasesParserTest.class.getResourceAsStream("/sdk-config.yaml")) {
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
    Set<String> databases = tested.convert(declarativeConfigProperties);
    assertEquals(new HashSet<>(Collections.singleton("postgresql")), databases);
  }

  @Test
  void configKey() {
    assertEquals("agent.sqlTagDatabases", tested.configKey());
  }
}
