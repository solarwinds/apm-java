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

package com.solarwinds.joboe.core.util;

import com.solarwinds.joboe.core.ReporterFactory;
import com.solarwinds.joboe.core.TestReporter;
import com.solarwinds.joboe.core.profiler.Profiler;
import com.solarwinds.joboe.core.profiler.ProfilerSetting;
import com.solarwinds.joboe.core.settings.*;
import com.solarwinds.joboe.sampling.SamplingConfiguration;
import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.joboe.sampling.SettingsArg;
import com.solarwinds.joboe.sampling.SettingsManager;
import com.solarwinds.joboe.sampling.TraceDecisionUtil;
import com.solarwinds.joboe.sampling.TracingMode;

public abstract class TestUtils {
  private static final TestDefaultSettings DEFAULT_SETTINGS = new TestDefaultSettings();

  private TestUtils() {}

  public static TestSettingsReader initSettingsReader() {
    TestSettingsReader reader = new TestSettingsReader();
    SettingsManager.initialize(
        new SimpleSettingsFetcher(reader), SamplingConfiguration.builder().build());
    return reader;
  }

  public static TestReporter initTraceReporter() {
    try {
      return ReporterFactory.getInstance().createTestReporter();
    } catch (Exception e) {
      return null;
    }
  }

  public static TestReporter initProfilingReporter(ProfilerSetting profilerSetting) {
    try {
      TestReporter testProfilingReporter = ReporterFactory.getInstance().createTestReporter();
      Profiler.initialize(profilerSetting, testProfilingReporter);
      return testProfilingReporter;
    } catch (Exception e) {
      return null;
    }
  }

  public static Settings getDefaultSettings() {
    return DEFAULT_SETTINGS;
  }

  private static class TestDefaultSettings extends Settings {
    public static final Double DEFAULT_BUCKET_CAPACITY = 16.0;
    public static final Double DEFAULT_BUCKET_RATE = 8.0;
    public static final long DEFAULT_SAMPLE_RATE = TraceDecisionUtil.SAMPLE_RESOLUTION; // 100%
    public static final short DEFAULT_TYPE = Settings.OBOE_SETTINGS_TYPE_DEFAULT_SAMPLE_RATE;

    @Override
    public long getValue() {
      return DEFAULT_SAMPLE_RATE;
    }

    @Override
    public long getTimestamp() {
      return TimeUtils.getTimestampMicroSeconds();
    }

    @Override
    public short getType() {
      return DEFAULT_TYPE;
    }

    @Override
    public short getFlags() {
      return TracingMode.ALWAYS.toFlags();
    }

    @Override
    public long getTtl() {
      return Integer.MAX_VALUE; // don't use long, otherwise it might overflow...
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getArgValue(SettingsArg<T> arg) {
      if (arg.equals(SettingsArg.BUCKET_CAPACITY)) {
        return (T) DEFAULT_BUCKET_CAPACITY;
      } else if (arg.equals(SettingsArg.BUCKET_RATE)) {
        return (T) DEFAULT_BUCKET_RATE;
      } else if (arg.equals(SettingsArg.RELAXED_BUCKET_CAPACITY)) {
        return (T) DEFAULT_BUCKET_CAPACITY;
      } else if (arg.equals(SettingsArg.RELAXED_BUCKET_RATE)) {
        return (T) DEFAULT_BUCKET_RATE;
      } else if (arg.equals(SettingsArg.STRICT_BUCKET_CAPACITY)) {
        return (T) DEFAULT_BUCKET_CAPACITY;
      } else if (arg.equals(SettingsArg.STRICT_BUCKET_RATE)) {
        return (T) DEFAULT_BUCKET_RATE;
      }

      return null;
    }
  }
}
