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

package com.solarwinds.opentelemetry.extensions.config;

import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.joboe.sampling.SettingsArg;
import java.util.List;
import java.util.stream.Collectors;

public class JsonSettingWrapper extends Settings {
  private final JsonSetting jsonSetting;

  public JsonSettingWrapper(JsonSetting jsonSetting) {
    this.jsonSetting = jsonSetting;
  }

  @Override
  public long getValue() {
    return jsonSetting.getValue();
  }

  @Override
  public long getTimestamp() {
    return jsonSetting.getTimestamp();
  }

  @Override
  public short getType() {
    return jsonSetting.getType();
  }

  @Override
  public short getFlags() {
    short flags = 0;
    String[] flagTokens = jsonSetting.getFlags().split(",");
    for (String flagToken : flagTokens) {
      if ("OVERRIDE".equals(flagToken)) {
        flags |= OBOE_SETTINGS_FLAG_OVERRIDE;
      } else if ("SAMPLE_START".equals(flagToken)) {
        flags |= OBOE_SETTINGS_FLAG_SAMPLE_START;
      } else if ("SAMPLE_THROUGH".equals(flagToken)) {
        flags |= OBOE_SETTINGS_FLAG_SAMPLE_THROUGH;
      } else if ("SAMPLE_THROUGH_ALWAYS".equals(flagToken)) {
        flags |= OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS;
      } else if ("TRIGGER_TRACE".equals(flagToken)) {
        flags |= OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED;
      } else if ("SAMPLE_BUCKET_ENABLED".equals(flagToken)) { // not used anymore
        flags |= OBOE_SETTINGS_FLAG_SAMPLE_BUCKET_ENABLED;
      } else {
        LoggerFactory.getLogger().debug("Unknown flag found from settings: " + flagToken);
      }
    }
    return flags;
  }

  @Override
  public long getTtl() {
    return jsonSetting.getTtl();
  }

  @Override
  public <T> T getArgValue(SettingsArg<T> settingsArg) {
    Object value = jsonSetting.getArguments().get(settingsArg.getKey());
    return settingsArg.readValue(value);
  }

  public static List<Settings> fromJsonSettings(List<JsonSetting> jsonSettings) {
    return jsonSettings.stream().map(JsonSettingWrapper::new).collect(Collectors.toList());
  }

  public static Settings wrap(JsonSetting jsonSettings) {
    return new JsonSettingWrapper(jsonSettings);
  }
}
