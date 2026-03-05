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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Reads from system environment variables for {@link ConfigProperty}
 *
 * @author Patson Luk
 */
public class EnvConfigReader extends ConfigReader {
  private final Map<String, String> env;

  public EnvConfigReader(Map<String, String> env) {
    super(ConfigSourceType.ENV_VAR);
    this.env = env;
  }

  @Override
  public void read(ConfigContainer container) throws InvalidConfigException {
    List<InvalidConfigException> exceptions = new ArrayList<InvalidConfigException>();
    for (Entry<String, ConfigProperty> envNameEntry :
        ConfigProperty.getEnviromentVariableMap().entrySet()) {
      String envName = envNameEntry.getKey();
      if (env.containsKey(envName)) {
        String value = env.get(envName);
        try {
          container.putByStringValue(envNameEntry.getValue(), value);

          String maskedValue;
          if (envNameEntry.getValue() == ConfigProperty.AGENT_SERVICE_KEY) {
            maskedValue = ServiceKeyUtils.maskServiceKey(value);
          } else {
            maskedValue = value;
          }
          logger.info(
              "System environment variable ["
                  + envName
                  + "] value ["
                  + maskedValue
                  + "] maps to agent property "
                  + envNameEntry.getValue());
        } catch (InvalidConfigException e) {
          logger.warn(
              "Invalid System environment variable [" + envName + "] value [" + value + "]");
          exceptions.add(e);
        }
      }
    }

    if (!exceptions.isEmpty()) {
      logger.warn(
          "Found "
              + exceptions.size()
              + " exception(s) while reading config from environment variables");
      throw exceptions.get(0); // report the first exception encountered
    }
  }
}
