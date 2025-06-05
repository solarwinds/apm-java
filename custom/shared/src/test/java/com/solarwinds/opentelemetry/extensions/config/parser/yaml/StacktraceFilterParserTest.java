package com.solarwinds.opentelemetry.extensions.config.parser.yaml;

import static org.junit.jupiter.api.Assertions.*;

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

class StacktraceFilterParserTest {
  private static DeclarativeConfigProperties declarativeConfigProperties;
  private final StacktraceFilterParser tested = new StacktraceFilterParser();

  @BeforeAll
  static void setup() {
    try (InputStream resourceAsStream =
        StacktraceFilterParserTest.class.getResourceAsStream("/sdk-config.yaml")) {
      DeclarativeConfigProperties configProperties =
          DeclarativeConfiguration.toConfigProperties(resourceAsStream);
      declarativeConfigProperties =
          configProperties
              .getStructured("instrumentation/development", DeclarativeConfigProperties.empty())
              .getStructured("java", DeclarativeConfigProperties.empty())
              .getStructured("solarwinds");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testConvert() throws InvalidConfigException {
    Set<String> attributes = tested.convert(declarativeConfigProperties);
    assertEquals(new HashSet<>(Collections.singleton("http.request.method")), attributes);
  }

  @Test
  void configKey() {
    assertEquals("agent.spanStacktraceFilters", tested.configKey());
  }
}
