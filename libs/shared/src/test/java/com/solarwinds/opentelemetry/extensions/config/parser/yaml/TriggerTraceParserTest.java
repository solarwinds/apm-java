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

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TriggerTraceParserTest {
  private static DeclarativeConfigProperties declarativeConfigProperties;
  private final TriggerTraceParser tested = new TriggerTraceParser();

  @BeforeAll
  static void setup() {
    try (InputStream resourceAsStream =
        TriggerTraceParserTest.class.getResourceAsStream("/sdk-config.yaml")) {
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
  void testConvertTrue() {
    Boolean result = tested.convert(declarativeConfigProperties);
    assertTrue(result);
  }

  @Test
  void testConvertNull() {
    Boolean result = tested.convert(DeclarativeConfigProperties.empty());
    assertTrue(result);
  }

  @Test
  void configKey() {
    assertEquals("agent.triggerTrace", tested.configKey());
  }
}
