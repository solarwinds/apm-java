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
import com.solarwinds.joboe.config.ConfigParser;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.logging.LogSetting;
import com.solarwinds.joboe.logging.Logger;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.nio.file.Path;
import java.nio.file.Paths;

@AutoService(ConfigParser.class)
public class LogSettingParser implements ConfigParser<DeclarativeConfigProperties, LogSetting> {
  private static final String CONFIG_KEY = "agent.logging";

  @Override
  public LogSetting convert(DeclarativeConfigProperties declarativeConfigProperties)
      throws InvalidConfigException {
    DeclarativeConfigProperties loggingSettings =
        declarativeConfigProperties.getStructured(CONFIG_KEY, DeclarativeConfigProperties.empty());
    DeclarativeConfigProperties fileInfo =
        loggingSettings.getStructured("file", DeclarativeConfigProperties.empty());

    Path filePath = null;
    String location = fileInfo.getString("location");
    if (location != null) {
      filePath = Paths.get(location);
    }

    return new LogSetting(
        Logger.Level.fromLabel(loggingSettings.getString("level", "info")),
        loggingSettings.getBoolean("stdout", true),
        loggingSettings.getBoolean("stderr", true),
        filePath,
        fileInfo.getInt("maxSize"),
        fileInfo.getInt("maxBackup"));
  }

  @Override
  public String configKey() {
    return CONFIG_KEY;
  }
}
