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
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.core.profiler.ProfilerSetting;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.shaded.org.json.JSONArray;
import com.solarwinds.joboe.shaded.org.json.JSONException;
import com.solarwinds.joboe.shaded.org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

public class ProfilerSettingParser implements ConfigParser<String, ProfilerSetting> {
  private static final Logger logger = LoggerFactory.getLogger();
  public static final String ENABLED_KEY = "enabled";
  public static final String EXCLUDE_PACKAGES_KEY = "excludePackages";
  public static final String INTERVAL_KEY = "interval";
  public static final String CIRCUIT_BREAKER_DURATION_THRESHOLD = "circuitBreakerDurationThreshold";
  public static final String CIRCUIT_BREAKER_COUNT_THRESHOLD = "circuitBreakerCountThreshold";

  public static final ProfilerSettingParser INSTANCE = new ProfilerSettingParser();

  private ProfilerSettingParser() {}

  @Override
  public ProfilerSetting convert(String javaValue) throws InvalidConfigException {
    try {
      JSONObject jsonObject = new JSONObject(javaValue);

      boolean isEnabled = jsonObject.getBoolean(ENABLED_KEY);

      int interval =
          jsonObject.has(INTERVAL_KEY)
              ? jsonObject.getInt(INTERVAL_KEY)
              : ProfilerSetting.DEFAULT_INTERVAL;

      Set<String> excludePackages;
      JSONArray excludePackagesArray = jsonObject.optJSONArray(EXCLUDE_PACKAGES_KEY);
      if (excludePackagesArray != null) {
        excludePackages = new HashSet<String>();
        for (int i = 0; i < excludePackagesArray.length(); i++) {
          excludePackages.add(excludePackagesArray.getString(i));
        }
      } else {
        excludePackages = ProfilerSetting.DEFAULT_EXCLUDE_PACKAGES;
      }

      if (interval != 0
          && interval
              < ProfilerSetting
                  .MIN_INTERVAL) { // special case for interval 0, it means profiler on standby
        throw new InvalidConfigException(
            "Profiling interval should be >= "
                + ProfilerSetting.MIN_INTERVAL
                + " but found "
                + interval);
      }

      int ciruitBreakerDurationThreshold =
          jsonObject.has(CIRCUIT_BREAKER_DURATION_THRESHOLD)
              ? jsonObject.getInt(CIRCUIT_BREAKER_DURATION_THRESHOLD)
              : ProfilerSetting.DEFAULT_CIRCUIT_BREAKER_DURATION_THRESHOLD;
      int circuitBreakerCountThreshold =
          jsonObject.has(CIRCUIT_BREAKER_COUNT_THRESHOLD)
              ? jsonObject.getInt(CIRCUIT_BREAKER_COUNT_THRESHOLD)
              : ProfilerSetting.DEFAULT_CIRCUIT_BREAKER_COUNT_THRESHOLD;

      return new ProfilerSetting(
          isEnabled,
          excludePackages,
          interval,
          ciruitBreakerDurationThreshold,
          circuitBreakerCountThreshold);
    } catch (JSONException e) {
      throw new InvalidConfigException(
          "Failed parsing profiler settings from config file: " + e.getMessage(), e);
    }
  }
}
