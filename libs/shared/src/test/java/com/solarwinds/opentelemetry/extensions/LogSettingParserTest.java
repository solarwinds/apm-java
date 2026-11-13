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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogSettingParserTest {

  @InjectMocks private LogSettingParser tested;

  @Test
  void returnLogSettingObjectGivenValidJson() throws InvalidConfigException {
    String json = "{\"level\": \"debug\", \"stdout\":\"enabled\", \"stderr\":\"enabled\"}";

    assertEquals(new LogSetting(Logger.Level.DEBUG, true, true), tested.convert(json));
  }

  @Test
  void ignoreFileSettingsAndKeepStdStreamsEnabled() throws InvalidConfigException {
    String json =
        "{"
            + "\"level\": \"info\","
            + "\"stdout\":\"disabled\","
            + "\"stderr\":\"disabled\","
            + "\"file\":{\"location\":\"hello.txt\",\"maxSize\":\"20\",\"maxBackup\":\"100\"}"
            + "}";

    assertEquals(new LogSetting(Logger.Level.INFO, true, true), tested.convert(json));
  }

  @Test
  void returnLogSettingWithStdStreamsDisabled() throws InvalidConfigException {
    String json = "{\"level\": \"warn\", \"stdout\":\"disabled\", \"stderr\":\"enabled\"}";

    assertEquals(new LogSetting(Logger.Level.WARNING, false, true), tested.convert(json));
  }

  @Test
  void stdStreamsDefaultToEnabled() throws InvalidConfigException {
    String json = "{\"level\": \"info\"}";

    assertEquals(new LogSetting(Logger.Level.INFO, true, true), tested.convert(json));
  }

  @Test
  void throwExceptionGivenUnknownKey() {
    String json = "{\"level\": \"info\", \"unknown\":\"value\"}";
    assertThrows(InvalidConfigException.class, () -> tested.convert(json));
  }

  @Test
  void returnLogSettingObjectWithInfoLogLevelGivenLogLevelString() throws InvalidConfigException {
    String json = "info";
    LogSetting actual = tested.convert(json);
    assertEquals(Logger.Level.INFO, actual.getLevel());
  }
}
