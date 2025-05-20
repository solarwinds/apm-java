package com.solarwinds.opentelemetry.extensions.initialize.config;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.ConfigParser;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.core.profiler.ProfilerSetting;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@AutoService(ConfigParser.class)
public class ProfilingSettingsParser
    implements ConfigParser<DeclarativeConfigProperties, ProfilerSetting> {
  @Override
  public ProfilerSetting convert(DeclarativeConfigProperties declarativeConfigProperties)
      throws InvalidConfigException {
    DeclarativeConfigProperties profilerSettings =
        declarativeConfigProperties.getStructured("profiler");
    if (profilerSettings == null) return null;

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
}
