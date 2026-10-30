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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.config.ConfigContainer;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.logging.LogSetting;
import com.solarwinds.joboe.logging.Logger;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeclarativeConfigParserTest {

  @Mock private DeclarativeConfigProperties declarativeConfigPropertiesMock;

  @Test
  void throwForUnknownConfig() {
    DeclarativeConfigParser parser = new DeclarativeConfigParser(new ConfigContainer());
    Set<String> keys = new HashSet<>(Arrays.asList("unknown-key1", "key2"));

    when(declarativeConfigPropertiesMock.getPropertyKeys()).thenReturn(keys);
    assertThrows(InvalidConfigException.class, () -> parser.parse(declarativeConfigPropertiesMock));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "agent.logging",
        "agent.serviceKey",
        "agent.collector",
        "agent.sqlTagPrepared",
        "agent.collectorTimeout",
        "agent.configFileWatchPeriod"
      })
  void throwForKeysOtherThanLogging(String key) {
    ConfigContainer configContainer = new ConfigContainer();
    DeclarativeConfigParser parser = new DeclarativeConfigParser(configContainer);

    when(declarativeConfigPropertiesMock.getPropertyKeys()).thenReturn(Collections.singleton(key));
    assertThrows(InvalidConfigException.class, () -> parser.parse(declarativeConfigPropertiesMock));
    assertNull(configContainer.get(ConfigProperty.fromConfigFileKey(key)));
  }

  @Test
  void testStructuredParsing() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    DeclarativeConfigParser parser = new DeclarativeConfigParser(configContainer);
    Set<String> keys = new HashSet<>(Collections.singletonList("logging"));

    when(declarativeConfigPropertiesMock.getPropertyKeys()).thenReturn(keys);
    when(declarativeConfigPropertiesMock.getStructured(any(), any()))
        .thenReturn(declarativeConfigPropertiesMock);

    when(declarativeConfigPropertiesMock.getString(any())).thenReturn(null);
    when(declarativeConfigPropertiesMock.getString(eq("level"), any())).thenReturn("debug");
    parser.parse(declarativeConfigPropertiesMock);

    verify(declarativeConfigPropertiesMock, atLeastOnce()).getStructured(eq("logging"), any());

    LogSetting logSetting = (LogSetting) configContainer.get(ConfigProperty.AGENT_LOGGING);
    assertEquals(Logger.Level.DEBUG, logSetting.getLevel());
  }
}
