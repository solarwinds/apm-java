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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.core.profiler.ProfilerSetting;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ProfilingSettingsParserTest {
  private final ProfilingSettingsParser tested = new ProfilingSettingsParser();

  @Test
  void testConvert() throws InvalidConfigException {
    try (InputStream resourceAsStream =
        ProfilingSettingsParserTest.class.getResourceAsStream("/sdk-config.yaml")) {
      DeclarativeConfigProperties configProperties =
          DeclarativeConfiguration.toConfigProperties(resourceAsStream);

      DeclarativeConfigProperties declarativeConfigProperties =
          configProperties
              .getStructured("instrumentation/development", DeclarativeConfigProperties.empty())
              .getStructured("java", DeclarativeConfigProperties.empty())
              .getStructured("solarwinds", DeclarativeConfigProperties.empty());

      ProfilerSetting profilerSetting = tested.convert(declarativeConfigProperties);
      assertEquals(new ProfilerSetting(true, Collections.emptySet(), 20, 100, 2), profilerSetting);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testConvertWithException() {
    try (InputStream resourceAsStream =
        ProfilingSettingsParserTest.class.getResourceAsStream(
            "/sdk-config-bad-transaction-settings-0.yaml")) {
      DeclarativeConfigProperties configProperties =
          DeclarativeConfiguration.toConfigProperties(resourceAsStream);

      DeclarativeConfigProperties declarativeConfigProperties =
          configProperties
              .getStructured("instrumentation/development", DeclarativeConfigProperties.empty())
              .getStructured("java", DeclarativeConfigProperties.empty())
              .getStructured("solarwinds", DeclarativeConfigProperties.empty());

      assertThrows(InvalidConfigException.class, () -> tested.convert(declarativeConfigProperties));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void configKey() {
    assertEquals("profiler", tested.configKey());
  }
}
