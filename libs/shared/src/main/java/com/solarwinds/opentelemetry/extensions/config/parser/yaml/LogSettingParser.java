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

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.logging.LogSetting;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;

@AutoService(DeclarativeConfigPropertyParser.class)
public class LogSettingParser implements DeclarativeConfigPropertyParser {
  private static final String CONFIG_KEY = "logging";

  @Override
  public LogSetting convert(DeclarativeConfigProperties declarativeConfigProperties)
      throws InvalidConfigException {
    DeclarativeConfigProperties loggingSettings =
        declarativeConfigProperties.getStructured(CONFIG_KEY, DeclarativeConfigProperties.empty());
    Logger.Level level = Logger.Level.fromLabel(loggingSettings.getString("level", "info"));
    if (loggingSettings.getPropertyKeys().contains("file")) {
      LoggerFactory.getLogger()
          .warn(
              "Logging to a file is no longer supported, ignoring logging.file. Agent logs are"
                  + " written to stdout and stderr.");
      // stdout/stderr may have been disabled in favor of the file, so keep logs from going silent
      return new LogSetting(level, true, true);
    }

    return new LogSetting(
        level,
        loggingSettings.getBoolean("stdout", true),
        loggingSettings.getBoolean("stderr", true));
  }

  @Override
  public String configKey() {
    return CONFIG_KEY;
  }
}
