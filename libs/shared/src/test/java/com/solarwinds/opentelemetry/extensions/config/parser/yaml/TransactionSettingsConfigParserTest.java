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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.sampling.TraceConfigs;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionSettingsConfigParserTest {
  private final TransactionSettingsConfigParser tested = new TransactionSettingsConfigParser();

  @Test
  void testConvert() throws InvalidConfigException {
    try (InputStream resourceAsStream =
        TransactionSettingsConfigParserTest.class.getResourceAsStream("/sdk-config.yaml")) {
      DeclarativeConfigProperties configProperties =
          DeclarativeConfiguration.toConfigProperties(resourceAsStream);
      DeclarativeConfigProperties declarativeConfigProperties =
          configProperties
              .getStructured("instrumentation/development", DeclarativeConfigProperties.empty())
              .getStructured("java", DeclarativeConfigProperties.empty())
              .getStructured("solarwinds", DeclarativeConfigProperties.empty());

      TraceConfigs traceConfigs = tested.convert(declarativeConfigProperties);
      assertNotNull(traceConfigs);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @ParameterizedTest
  @MethodSource("paths")
  void testConvertWithException(String configPath) {
    try (InputStream resourceAsStream =
        TransactionSettingsConfigParserTest.class.getResourceAsStream(configPath)) {
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
    assertEquals("agent.transactionSettings", tested.configKey());
  }

  private Stream<Arguments> paths() {
    return Stream.of(
        Arguments.of("/sdk-config-bad-transaction-settings-0.yaml"),
        Arguments.of("/sdk-config-bad-transaction-settings-1.yaml"));
  }
}
