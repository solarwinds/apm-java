package com.solarwinds.opentelemetry.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.logging.LogSetting;
import com.solarwinds.joboe.logging.Logger;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogSettingParserTest {

  @InjectMocks private LogSettingParser tested;

  @Test
  void returnLogSettingObjectGivenValidJson() throws InvalidConfigException {
    String json =
        "{"
            + "\"level\": \"info\","
            + "\"stdout\":\"enabled\","
            + "\"stderr\":\"disabled\","
            + "\"file\":{\"location\":\"hello.txt\",\"maxSize\":\"20\",\"maxBackup\":\"100\"}"
            + "}";

    LogSetting expected =
        new LogSetting(Logger.Level.INFO, true, false, Paths.get("hello.txt"), 20, 100);
    LogSetting actual = tested.convert(json);
    assertEquals(expected, actual);
  }

  @Test
  void throwExceptionGivenInvalidJsonConfig() throws InvalidConfigException {
    String json =
        "{"
            + "\"level\": \"info\","
            + "\"stdout\":\"enabled\","
            + "\"stderr\":\"disabled\","
            + "\"file\":{}"
            + "}";
    assertThrows(InvalidConfigException.class, () -> tested.convert(json));
  }

  @Test
  void returnLogSettingObjectWithInfoLogLevelGivenLogLevelString() throws InvalidConfigException {
    String json = "info";
    LogSetting actual = tested.convert(json);
    assertEquals(Logger.Level.INFO, actual.getLevel());
  }
}
