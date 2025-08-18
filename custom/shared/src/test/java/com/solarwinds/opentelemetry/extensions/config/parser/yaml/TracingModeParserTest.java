package com.solarwinds.opentelemetry.extensions.config.parser.yaml;

import static org.junit.jupiter.api.Assertions.*;

import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.sampling.TracingMode;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TracingModeParserTest {
  private static DeclarativeConfigProperties declarativeConfigProperties;
  private final TracingModeParser tested = new TracingModeParser();

  @BeforeAll
  static void setup() {
    try (InputStream resourceAsStream =
        TracingModeParserTest.class.getResourceAsStream("/sdk-config.yaml")) {
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
    TracingMode tracingMode = tested.convert(declarativeConfigProperties);
    assertEquals(TracingMode.ENABLED, tracingMode);
  }

  @Test
  void configKey() {
    assertEquals("agent.tracingMode", tested.configKey());
  }
}
