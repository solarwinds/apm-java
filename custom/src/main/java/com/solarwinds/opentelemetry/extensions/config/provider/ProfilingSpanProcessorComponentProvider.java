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

package com.solarwinds.opentelemetry.extensions.config.provider;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.config.JavaRuntimeVersionChecker;
import com.solarwinds.joboe.config.ProxyConfig;
import com.solarwinds.joboe.core.profiler.ProfilerSetting;
import com.solarwinds.opentelemetry.extensions.SolarwindsProfilingSpanProcessor;
import com.solarwinds.opentelemetry.extensions.config.ProxyHelper;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@AutoService(ComponentProvider.class)
public class ProfilingSpanProcessorComponentProvider implements ComponentProvider {

  public static final String COMPONENT_NAME = "swo_profiler";

  private static final List<String> DEFAULT_EXCLUDE_PACKAGES =
      Arrays.asList("java", "javax", "com.sun", "sun", "sunw");

  @Override
  public Class<SpanProcessor> getType() {
    return SpanProcessor.class;
  }

  @Override
  public String getName() {
    return COMPONENT_NAME;
  }

  @Override
  public SpanProcessor create(DeclarativeConfigProperties declarativeConfigProperties) {
    try {
      ProfilerSetting profilerSetting = convert(declarativeConfigProperties);
      ConfigManager.setConfig(ConfigProperty.PROFILER, profilerSetting);
    } catch (InvalidConfigException e) {
      throw new RuntimeException(e);
    }

    return new SolarwindsProfilingSpanProcessor();
  }

  ProfilerSetting convert(DeclarativeConfigProperties profilerSettings)
      throws InvalidConfigException {

    boolean enabled =
        JavaRuntimeVersionChecker.isJdkVersionSupported()
            && profilerSettings.getBoolean("enabled", false);
    int circuitBreakerDurationThreshold =
        profilerSettings.getInt("circuit_breaker_duration_threshold", 100);
    int circuitBreakerCountThreshold =
        profilerSettings.getInt("circuit_breaker_count_threshold", 2);

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

    ProxyConfig proxyConfig = ProxyHelper.toProxyConfig(profilerSettings);
    if (proxyConfig != null) {
      ConfigManager.setConfig(ConfigProperty.AGENT_PROXY, proxyConfig);
    }

    List<String> excludePackages =
        profilerSettings.getScalarList("exclude_packages", String.class, DEFAULT_EXCLUDE_PACKAGES);
    return new ProfilerSetting(
        enabled,
        new HashSet<>(excludePackages),
        interval,
        circuitBreakerDurationThreshold,
        circuitBreakerCountThreshold);
  }
}
