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

import static org.junit.jupiter.api.Assertions.*;

import com.solarwinds.joboe.config.InvalidConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SqlQueryMaxLengthParserTest {
  private static DeclarativeConfigProperties declarativeConfigProperties;
  private final SqlQueryMaxLengthParser tested = new SqlQueryMaxLengthParser();

  @BeforeAll
  static void setup() {
    try (InputStream resourceAsStream =
        SqlQueryMaxLengthParserTest.class.getResourceAsStream("/sdk-config.yaml")) {
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
  void testConvertValidValue() throws InvalidConfigException {
    Integer result = tested.convert(declarativeConfigProperties);
    assertEquals(4096, result);
  }

  @Test
  void testConvertNull() throws InvalidConfigException {
    Integer result = tested.convert(DeclarativeConfigProperties.empty());
    assertNull(result);
  }

  @Test
  void testConvertBelowRange() {
    assertThrows(InvalidConfigException.class, () -> tested.convert(propsWithValue(100)));
  }

  @Test
  void testConvertAboveRange() {
    assertThrows(InvalidConfigException.class, () -> tested.convert(propsWithValue(200000)));
  }

  @Test
  void configKey() {
    assertEquals("agent.sqlQueryMaxLength", tested.configKey());
  }

  private DeclarativeConfigProperties propsWithValue(int value) {
    String yaml = "agent.sqlQueryMaxLength: " + value + "\n";
    return DeclarativeConfiguration.toConfigProperties(
        new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
  }
}
