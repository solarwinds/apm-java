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

public class InvalidConfigException extends Exception {
  private static final long serialVersionUID = 1L;
  protected String originalMessage;
  @Getter protected ConfigProperty configProperty;

  public InvalidConfigException(String message) {
    this(message, null);
  }

  public InvalidConfigException(Throwable cause) {
    this(null, cause);
  }

  public InvalidConfigException(String message, Throwable cause) {
    this(null, message, cause);
  }

  public InvalidConfigException(ConfigProperty configProperty, String message) {
    this(configProperty, message, null);
  }

  public InvalidConfigException(ConfigProperty configProperty, String message, Throwable cause) {
    super(message, cause);
    this.originalMessage = message;
    this.configProperty = configProperty;
  }

  @Override
  public String getMessage() {
    if (configProperty == null) {
      return super.getMessage();
    } else {
      return "Found error in config. Config key: "
          + configProperty.name()
          + " "
          + super.getMessage();
    }
  }
}
