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

package com.solarwinds.opentelemetry.extensions.config.parser.json;

import com.solarwinds.joboe.config.ConfigParser;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.logging.LogSanitizer;
import com.solarwinds.joboe.logging.LogSetting;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class LogSettingParser implements ConfigParser<String, LogSetting> {
  private static final Logger logger = LoggerFactory.getLogger();
  public static final String LEVEL_KEY = "level";
  public static final String STDOUT_KEY = "stdout";
  public static final String STDERR_KEY = "stderr";
  public static final String FILE_KEY = "file";

  private static final List<String> VALID_CONFIG_KEYS =
      Arrays.asList(LEVEL_KEY, STDERR_KEY, STDOUT_KEY, FILE_KEY);

  public static final LogSettingParser INSTANCE = new LogSettingParser();
  static final boolean DEFAULT_STDOUT_ENABLED = true;
  static final boolean DEFAULT_STDERR_ENABLED = true;

  private LogSettingParser() {}

  @Override
  public LogSetting convert(String javaValue) throws InvalidConfigException {
    String stringValue = javaValue.trim();
    if (javaValue.startsWith("{")) {
      return convertJsonValue(stringValue);
    } else {
      return convertStringValue(stringValue);
    }
  }

  private LogSetting convertStringValue(String stringValue) {
    Logger.Level level = Logger.Level.fromLabel(stringValue);
    // keep the existing behavior, not throwing exceptions on null level (invalid string)
    if (level == null) { // but do warn about it
      logger.warn(
          "Agent logging level value ["
              + LogSanitizer.sanitize(stringValue)
              + "] is invalid. Using default logging level instead...");
    }

    return new LogSetting(level, true, true);
  }

  private LogSetting convertJsonValue(String stringValue) throws InvalidConfigException {
    try {
      JSONObject jsonObject = new JSONObject(stringValue);

      checkUnknownKeys(jsonObject, VALID_CONFIG_KEYS);

      Logger.Level level = Logger.Level.fromLabel(jsonObject.getString(LEVEL_KEY));
      // since this is the new format, for invalid level value, we can throw exception
      if (level == null) {
        throw new InvalidConfigException(
            "Agent logging level value ["
                + stringValue
                + "] defined in "
                + getKeyDisplayString(LEVEL_KEY)
                + " is invalid");
      }

      boolean stdOutEnabled = getEnabled(jsonObject, STDOUT_KEY, DEFAULT_STDOUT_ENABLED);
      boolean stdErrEnabled = getEnabled(jsonObject, STDERR_KEY, DEFAULT_STDERR_ENABLED);

      if (jsonObject.has(FILE_KEY)) {
        // stdout/stderr could only be disabled when a file was set, so keep logs from going silent
        logger.warn(
            "Logging to a file is no longer supported, ignoring "
                + getKeyDisplayString(FILE_KEY)
                + ". Agent logs are written to stdout and stderr.");
        return new LogSetting(level, true, true);
      }

      return new LogSetting(level, stdOutEnabled, stdErrEnabled);
    } catch (JSONException e) {
      throw new InvalidConfigException(
          "Failed parsing log settings from config file: " + e.getMessage(), e);
    }
  }

  private static void checkUnknownKeys(JSONObject jsonObject, List<String> validKeys)
      throws InvalidConfigException {
    List<String> unknownKeys = new ArrayList<String>(jsonObject.keySet());
    unknownKeys.removeAll(validKeys);

    if (!unknownKeys.isEmpty()) {
      throw new InvalidConfigException(
          "Found unknown key(s) ["
              + String.join(", ", unknownKeys)
              + "], valid keys are ["
              + String.join(", ", validKeys)
              + "]");
    }
  }

  private boolean getEnabled(JSONObject jsonObject, String key, boolean defaultValue)
      throws InvalidConfigException {
    try {
      String stringValue = jsonObject.optString(key, null);
      if (stringValue == null) {
        return defaultValue;
      } else {
        return ModeStringToBooleanParser.INSTANCE.convert(stringValue);
      }
    } catch (InvalidConfigException e) {
      throw new InvalidConfigException(
          "Failed to parse " + getKeyDisplayString(key) + " : " + e.getMessage(), e);
    }
  }

  private String getKeyDisplayString(String key) {
    return "[" + key + "] of [" + ConfigProperty.AGENT_LOGGING.getConfigFileKey() + "]";
  }
}
