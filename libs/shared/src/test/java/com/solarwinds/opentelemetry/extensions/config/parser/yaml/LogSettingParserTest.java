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

package com.solarwinds.opentelemetry.extensions.config.parser.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.logging.LogSetting;
import com.solarwinds.joboe.logging.Logger;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
              .getStructured("distribution", DeclarativeConfigProperties.empty())
              .getStructured("solarwinds");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testConvert() throws InvalidConfigException {
    LogSetting logSetting = tested.convert(declarativeConfigProperties);
    assertEquals(new LogSetting(Logger.Level.TRACE, true, true), logSetting);
  }

  @Test
  void testConvertIgnoresFileSettingsAndKeepsStdStreamsEnabled() throws InvalidConfigException {
    LogSetting logSetting =
        tested.convert(
            fromYaml(
                "logging:\n"
                    + "  level: debug\n"
                    + "  stdout: false\n"
                    + "  stderr: false\n"
                    + "  file:\n"
                    + "    location: /tmp/agent.log\n"
                    + "    max_size: 20\n"
                    + "    max_backup: 3\n"));

    assertEquals(new LogSetting(Logger.Level.DEBUG, true, true), logSetting);
  }

  @Test
  void testConvertWithStdStreamSettings() throws InvalidConfigException {
    LogSetting logSetting =
        tested.convert(
            fromYaml("logging:\n" + "  level: warn\n" + "  stdout: false\n" + "  stderr: true\n"));

    assertEquals(new LogSetting(Logger.Level.WARNING, false, true), logSetting);
  }

  @Test
  void configKey() {
    assertEquals("logging", tested.configKey());
  }

  private static DeclarativeConfigProperties fromYaml(String yaml) {
    return DeclarativeConfiguration.toConfigProperties(
        new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
  }
}
