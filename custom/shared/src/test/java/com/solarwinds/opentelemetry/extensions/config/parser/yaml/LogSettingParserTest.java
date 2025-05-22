package com.solarwinds.opentelemetry.extensions.config.parser.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.logging.LogSetting;
import com.solarwinds.joboe.logging.Logger;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LogSettingParserTest {
  private static DeclarativeConfigProperties declarativeConfigProperties;
  private final LogSettingParser tested = new LogSettingParser();

  @BeforeAll
  static void setup() {
    try (InputStream resourceAsStream =
        LogSettingParserTest.class.getResourceAsStream("/sdk-config.yaml")) {
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
    LogSetting logSetting = tested.convert(declarativeConfigProperties);
    assertEquals(new LogSetting(Logger.Level.TRACE, true, true, null, 10, 5), logSetting);
  }

  @Test
  void configKey() {
    assertEquals("agent.logging", tested.configKey());
  }
}
