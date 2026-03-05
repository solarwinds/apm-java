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

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;

public class ConfigManager {
  private static final Logger logger = LoggerFactory.getLogger();
  private ConfigContainer configs;

  private static final ConfigManager SINGLETON = new ConfigManager(); // only a singleton for now

  private ConfigManager() {}

  public static void initialize(ConfigContainer configs) {
    SINGLETON.configs = configs;
  }

  /** For internal testing - reset the states of the ConfigManager */
  public static void reset() {
    SINGLETON.configs = null;
  }

  public static void setConfig(ConfigProperty configKey, Object value)
      throws InvalidConfigException {
    if (SINGLETON.configs == null) {
      SINGLETON.configs = new ConfigContainer();
    }
    SINGLETON.configs.put(configKey, value, true);
  }

  public static void removeConfig(ConfigProperty configKey) {
    if (SINGLETON.configs != null) {
      SINGLETON.configs.remove(configKey);
    }
  }

  /**
   * Convenience method for other code to read the configuration value of the Agent
   *
   * @param configKey
   * @return the configuration value of the provided key. Take note that this might be null if the
   *     configuration property is not required
   */
  public static Object getConfig(ConfigProperty configKey) {
    if (SINGLETON.configs == null) {
      logger.debug(
          "Failed to read config property ["
              + configKey
              + "] as agent is not initialized properly, config is null!");
      return null;
    }

    return SINGLETON.configs.get(configKey);
  }

  @SuppressWarnings("unchecked")
  public static <T> T getConfigOptional(ConfigProperty configKey, T defaultValue) {
    if (SINGLETON.configs == null) {
      logger.debug(
          "Failed to read config property ["
              + configKey
              + "] as agent is not initialized properly, config is null!");
      return defaultValue;
    }
    Object value = SINGLETON.configs.get(configKey);
    return value != null ? (T) value : defaultValue;
  }

  public static ConfigContainer getConfigs(ConfigGroup... groups) {
    if (SINGLETON.configs == null) {
      logger.debug("Agent is not initialized properly, config is null!");
      return new ConfigContainer();
    }

    return SINGLETON.configs.subset(groups);
  }
}
