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

import lombok.Getter;

/**
 * Invalid config while reading from a specific {@Link ConfigSourceType}
 *
 * <p>This contains extra info on the source type read and a ConfigContainer with the config read so
 * far
 */
public class InvalidConfigReadSourceException extends InvalidConfigException {
  private static final long serialVersionUID = 1L;
  private final ConfigSourceType configSourceType;
  @Getter private ConfigContainer configContainerBeforeException = null;
  private String physicalLocation = null;

  public InvalidConfigReadSourceException(
      ConfigProperty configProperty,
      ConfigSourceType sourceType,
      String physicalLocation,
      ConfigContainer configContainerBeforeException,
      InvalidConfigException exception) {
    super(configProperty, exception.originalMessage, exception);
    this.physicalLocation = physicalLocation;
    this.configSourceType = sourceType;
    this.configContainerBeforeException = configContainerBeforeException;
    this.configProperty = exception.getConfigProperty();
  }

  @Override
  public String getMessage() {
    if (configProperty == null && configSourceType == null) {
      return super.getMessage();
    } else {
      StringBuilder message = new StringBuilder("Found error in config. ");
      if (configSourceType != null) {
        message.append("Location: " + getLocation(configSourceType, physicalLocation) + ".");
      }
      if (configProperty != null) {
        message.append(
            "Config key: "
                + (configSourceType != null
                    ? getConfigPropertyLabel(configSourceType, configProperty)
                    : configProperty.name())
                + ".");
      }

      message.append(" " + originalMessage);
      return message.toString();
    }
  }

  private static String getConfigPropertyLabel(
      ConfigSourceType configSourceType, ConfigProperty configProperty) {
    switch (configSourceType) {
      case ENV_VAR:
        return configProperty.getEnvironmentVariableKey();
      case JSON_FILE:
        return configProperty.getConfigFileKey();
      default:
        return configProperty.name();
    }
  }

  private static String getLocation(ConfigSourceType configSourceType, String physicalLocation) {
    switch (configSourceType) {
      case ENV_VAR:
        return "Environment variable";
      case JSON_FILE:
        return "JSON config file at " + physicalLocation;
      default:
        return "Unknown location";
    }
  }
}
