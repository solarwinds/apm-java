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

package com.solarwinds.joboe.core.profiler;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
public class ProfilerSetting implements Serializable {
  public static final int DEFAULT_INTERVAL = 20;
  public static final int MIN_INTERVAL = 10;
  public static final int DEFAULT_CIRCUIT_BREAKER_DURATION_THRESHOLD = 100;
  public static final int DEFAULT_CIRCUIT_BREAKER_COUNT_THRESHOLD = 2;
  public static final Set<String> DEFAULT_EXCLUDE_PACKAGES =
      new HashSet<String>(Arrays.asList("java", "javax", "com.sun", "sun", "sunw"));
  private final boolean isEnabled;
  @Getter private final Set<String> excludePackages;
  @Getter private final int interval;
  @Getter private final int circuitBreakerDurationThreshold;
  @Getter private final int circuitBreakerCountThreshold;

  public ProfilerSetting(
      boolean isEnabled,
      Set<String> excludePackages,
      int interval,
      int circuitBreakerDurationThreshold,
      int circuitBreakerCountThreshold) {
    super();
    this.isEnabled = isEnabled;
    this.excludePackages = excludePackages;
    this.interval = interval;
    this.circuitBreakerDurationThreshold = circuitBreakerDurationThreshold;
    this.circuitBreakerCountThreshold = circuitBreakerCountThreshold;
  }

  public ProfilerSetting(boolean isEnabled, int interval) {
    this(
        isEnabled,
        DEFAULT_EXCLUDE_PACKAGES,
        interval,
        DEFAULT_CIRCUIT_BREAKER_DURATION_THRESHOLD,
        DEFAULT_CIRCUIT_BREAKER_COUNT_THRESHOLD);
  }

  public boolean isEnabled() {
    return isEnabled;
  }

  @Override
  public String toString() {
    return "ProfilerSetting [isEnabled="
        + isEnabled
        + ", excludePackages="
        + excludePackages
        + ", interval="
        + interval
        + ", circuitBreakerDurationThreshold="
        + circuitBreakerDurationThreshold
        + ", circuitBreakerCountThreshold="
        + circuitBreakerCountThreshold
        + "]";
  }
}
