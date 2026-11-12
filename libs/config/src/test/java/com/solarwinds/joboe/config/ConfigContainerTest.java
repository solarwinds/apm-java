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

package com.solarwinds.joboe.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConfigContainerTest {

  @Test
  void putByStringValueSanitizesFreeFormText() throws InvalidConfigException {
    ConfigContainer container = new ConfigContainer();
    container.putByStringValue(ConfigProperty.AGENT_HOSTNAME_ALIAS, "host\r\nINFO forged");

    assertEquals("host\\r\\nINFO forged", container.get(ConfigProperty.AGENT_HOSTNAME_ALIAS));
  }

  @Test
  void putSanitizesFreeFormTextWhenOverriding() throws InvalidConfigException {
    ConfigContainer container = new ConfigContainer();
    container.put(ConfigProperty.AGENT_COLLECTOR, "apm.collector.example.com");
    container.put(ConfigProperty.AGENT_COLLECTOR, "apm.collector.example.com\nWARN forged", true);

    assertEquals(
        "apm.collector.example.com\\nWARN forged", container.get(ConfigProperty.AGENT_COLLECTOR));
  }

  @Test
  void putKeepsCleanAndNonStringValuesAsIs() throws InvalidConfigException {
    ConfigContainer container = new ConfigContainer();
    container.put(ConfigProperty.AGENT_COLLECTOR, "apm.collector.example.com:443");
    container.putByStringValue(ConfigProperty.AGENT_SQL_QUERY_MAX_LENGTH, "1024");

    assertEquals("apm.collector.example.com:443", container.get(ConfigProperty.AGENT_COLLECTOR));
    assertEquals(1024, container.get(ConfigProperty.AGENT_SQL_QUERY_MAX_LENGTH));
  }
}
