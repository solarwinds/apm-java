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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigManagerTest {

  @BeforeEach
  void setUp() {
    ConfigManager.reset();
  }

  @AfterEach
  void tearDown() {
    ConfigManager.reset();
  }

  @Test
  void initializeSetsConfigsWhenUninitialized() throws InvalidConfigException {
    ConfigContainer configs = new ConfigContainer();
    configs.put(ConfigProperty.AGENT_COLLECTOR, "apm.collector.com");

    ConfigManager.initialize(configs);
    assertEquals("apm.collector.com", ConfigManager.getConfig(ConfigProperty.AGENT_COLLECTOR));
  }

  @Test
  void initializeKeepsPreviouslySetConfigs() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_COLLECTOR, "apm.collector.com");
    ConfigContainer configs = new ConfigContainer();
    configs.put(ConfigProperty.AGENT_SERVICE_KEY, "token:service");

    ConfigManager.initialize(configs);
    assertEquals("apm.collector.com", ConfigManager.getConfig(ConfigProperty.AGENT_COLLECTOR));
    assertEquals("token:service", ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY));
  }

  @Test
  void initializeOverridesPreviouslySetConfigsOnConflict() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_COLLECTOR, "old.collector.com");
    ConfigContainer configs = new ConfigContainer();
    configs.put(ConfigProperty.AGENT_COLLECTOR, "new.collector.com");

    ConfigManager.initialize(configs);
    assertEquals("new.collector.com", ConfigManager.getConfig(ConfigProperty.AGENT_COLLECTOR));
  }

  @Test
  void initializeMergesSubsetWithMissingGroup() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_COLLECTOR, "apm.collector.com");

    ConfigManager.initialize(new ConfigContainer().subset(ConfigGroup.AGENT));
    assertEquals("apm.collector.com", ConfigManager.getConfig(ConfigProperty.AGENT_COLLECTOR));
  }
}
