/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solarwinds.opentelemetry.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.logging.LogSetting;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.opentelemetry.extensions.config.parser.json.LogSettingParser;
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
