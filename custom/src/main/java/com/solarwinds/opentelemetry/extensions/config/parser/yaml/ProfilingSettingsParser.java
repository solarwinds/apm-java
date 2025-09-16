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
import com.solarwinds.joboe.core.profiler.ProfilerSetting;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@SuppressWarnings("rawtypes")
@AutoService(ConfigParser.class)
public class ProfilingSettingsParser
    implements ConfigParser<DeclarativeConfigProperties, ProfilerSetting> {
  private static final String CONFIG_KEY = "profiler";

  @Override
  public ProfilerSetting convert(DeclarativeConfigProperties declarativeConfigProperties)
      throws InvalidConfigException {
    DeclarativeConfigProperties profilerSettings =
        declarativeConfigProperties.getStructured(CONFIG_KEY);
    if (profilerSettings == null) {
      return null;
    }

    boolean enabled = profilerSettings.getBoolean("enabled", false);
    int circuitBreakerDurationThreshold =
        profilerSettings.getInt("circuitBreakerDurationThreshold", 100);
    int circuitBreakerCountThreshold = profilerSettings.getInt("circuitBreakerCountThreshold", 2);

    int interval = profilerSettings.getInt("interval", 20);
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

    List<String> excludePackages =
        profilerSettings.getScalarList("excludePackages", String.class, Collections.emptyList());
    return new ProfilerSetting(
        enabled,
        new HashSet<>(excludePackages),
        interval,
        circuitBreakerDurationThreshold,
        circuitBreakerCountThreshold);
  }

  @Override
  public String configKey() {
    return CONFIG_KEY;
  }
}
