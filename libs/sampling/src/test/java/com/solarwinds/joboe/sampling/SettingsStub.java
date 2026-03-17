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

package com.solarwinds.joboe.sampling;

import java.util.HashMap;
import java.util.Map;

public class SettingsStub extends Settings {
  private final short settingsType;
  private short flags = 0;
  private int sampleRate = 1000000;
  private final Map<SettingsArg<?>, Object> args;

  private SettingsStub(
      short settingsType, short flags, Integer sampleRate, Map<SettingsArg<?>, Object> args) {
    this.settingsType = settingsType;
    this.flags = flags;
    if (sampleRate != null) {
      this.sampleRate = sampleRate;
    }
    this.args = args;
  }

  @Override
  public long getValue() {
    return sampleRate;
  }

  @Override
  public long getTimestamp() {
    return 0;
  }

  @Override
  public short getFlags() {
    return flags;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getArgValue(SettingsArg<T> arg) {
    return (T) args.get(arg);
  }

  @Override
  public short getType() {
    return settingsType;
  }

  @Override
  public long getTtl() {
    return 0;
  }

  public static SettingsStubBuilder builder() {
    return new SettingsStubBuilder();
  }

  public static class SettingsStubBuilder {
    private short flags = 0;
    private Integer sampleRate = null;
    private final Map<SettingsArg<?>, Object> args = new HashMap<SettingsArg<?>, Object>();

    public static final double DEFAULT_TOKEN_BUCKET_RATE = 8.0;
    public static final double DEFAULT_TOKEN_BUCKET_CAPACITY = 16.0;
    private short settingsType = Settings.OBOE_SETTINGS_TYPE_DEFAULT_SAMPLE_RATE;

    public SettingsStubBuilder() {
      addDefaultArgs();
    }

    private void addDefaultArgs() {
      args.put(SettingsArg.BUCKET_RATE, DEFAULT_TOKEN_BUCKET_RATE);
      args.put(SettingsArg.BUCKET_CAPACITY, DEFAULT_TOKEN_BUCKET_CAPACITY);
      args.put(SettingsArg.RELAXED_BUCKET_RATE, DEFAULT_TOKEN_BUCKET_RATE);
      args.put(SettingsArg.RELAXED_BUCKET_CAPACITY, DEFAULT_TOKEN_BUCKET_CAPACITY);
      args.put(SettingsArg.STRICT_BUCKET_RATE, DEFAULT_TOKEN_BUCKET_RATE);
      args.put(SettingsArg.STRICT_BUCKET_CAPACITY, DEFAULT_TOKEN_BUCKET_CAPACITY);
    }

    public SettingsStubBuilder withFlags(
        boolean isStart,
        boolean isThrough,
        boolean isThroughAlways,
        boolean isTriggerTraceEnabled,
        boolean isOverride) {
      flags = getFlags(isStart, isThrough, isThroughAlways, isTriggerTraceEnabled, isOverride);
      return this;
    }

    public SettingsStubBuilder withFlags(TracingMode tracingMode) {
      flags |= tracingMode.toFlags();
      return this;
    }

    public SettingsStubBuilder withSampleRate(int sampleRate) {
      this.sampleRate = sampleRate;
      return this;
    }

    public <T> SettingsStubBuilder withSettingsArg(SettingsArg<T> arg, T value) {
      args.put(arg, value);
      return this;
    }

    public SettingsStubBuilder withSettingsArgs(Map<SettingsArg<?>, ?> args) {
      this.args.clear();
      this.args.putAll(args);
      return this;
    }

    public SettingsStubBuilder withSettingsType(short settingsType) {
      this.settingsType = settingsType;
      return this;
    }

    public SettingsStub build() {
      return new SettingsStub(settingsType, flags, sampleRate, args);
    }

    private static short getFlags(
        boolean isStart,
        boolean isThrough,
        boolean isThroughAlways,
        boolean isTriggerTraceEnabled,
        boolean isOverride) {
      byte flags = 0;
      if (isOverride) {
        flags |= Settings.OBOE_SETTINGS_FLAG_OVERRIDE;
      }

      if (isStart) {
        flags |= Settings.OBOE_SETTINGS_FLAG_SAMPLE_START;
      }

      if (isThrough) {
        flags |= Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH;
      }

      if (isThroughAlways) {
        flags |= Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS;
      }

      if (isTriggerTraceEnabled) {
        flags |= Settings.OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED;
      }

      return flags;
    }
  }
}
