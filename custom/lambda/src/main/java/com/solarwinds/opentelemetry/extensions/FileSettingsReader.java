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

package com.solarwinds.opentelemetry.extensions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.SamplingException;
import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.opentelemetry.extensions.config.JsonSetting;
import com.solarwinds.opentelemetry.extensions.config.JsonSettingWrapper;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FileSettingsReader {
  private final String settingsFilePath;

  private static final Logger logger = LoggerFactory.getLogger();

  private static final Gson gson = new GsonBuilder().create();
  private final Type type = new TypeToken<List<JsonSetting>>() {}.getType();

  public FileSettingsReader(String settingsFilePath) {
    this.settingsFilePath = settingsFilePath;
  }

  public Settings getSettings() throws SamplingException {
    Settings settings = null;
    try {
      byte[] bytes = Files.readAllBytes(Paths.get(settingsFilePath));
      List<Settings> kvSetting =
          JsonSettingWrapper.fromJsonSettings(gson.fromJson(new String(bytes), type));
      logger.debug(String.format("Got settings from file: %s", kvSetting));

      if (!kvSetting.isEmpty()) {
        settings = kvSetting.get(0);
      }

    } catch (IOException e) {
      logger.debug(String.format("Failed to read settings from file, error: %s", e));
      throw new SamplingException("Error reading settings from file");
    }
    return settings;
  }
}
