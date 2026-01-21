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

import com.solarwinds.joboe.config.ConfigContainer;
import com.solarwinds.joboe.config.ConfigParser;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

public final class DeclarativeConfigParser {
  private final ConfigContainer configContainer;

  private final Map<String, ConfigParser<DeclarativeConfigProperties, Object>> register =
      new HashMap<>();

  @SuppressWarnings("unchecked")
  public DeclarativeConfigParser(ConfigContainer configContainer) {
    this.configContainer = configContainer;
    for (ConfigParser<?, ?> parser :
        ServiceLoader.load(ConfigParser.class, DeclarativeConfigParser.class.getClassLoader())) {
      register.put(parser.configKey(), (ConfigParser<DeclarativeConfigProperties, Object>) parser);
    }
  }

  public void parse(DeclarativeConfigProperties declarativeConfigProperties)
      throws InvalidConfigException {
    Set<String> propertyKeys = declarativeConfigProperties.getPropertyKeys();
    Set<String> unknownKeys = new HashSet<>();

    for (String key : propertyKeys) {
      ConfigProperty configProperty = ConfigProperty.fromConfigFileKey(key);

      if (configProperty == null) {
        unknownKeys.add(key);
      } else {
        Object parsed = null;
        Class<?> typeClass = configProperty.getTypeClass();

        if (typeClass == String.class) {
          parsed = declarativeConfigProperties.getString(key);

        } else if (typeClass == Boolean.class) {
          parsed = declarativeConfigProperties.getBoolean(key);

        } else if (typeClass == Integer.class) {
          parsed = declarativeConfigProperties.getInt(key);

        } else if (typeClass == Long.class) {
          parsed = declarativeConfigProperties.getLong(key);
        }

        if (register.containsKey(key)) {
          ConfigParser<DeclarativeConfigProperties, Object> configParser = register.get(key);
          parsed = configParser.convert(declarativeConfigProperties);
        }

        if (parsed != null) {
          configContainer.put(configProperty, parsed);
        }
      }
    }

    if (!unknownKeys.isEmpty()) {
      throw new InvalidConfigException(
          String.format("Unknown keys found in solarwinds config. Keys -> %s", unknownKeys));
    }
  }
}
