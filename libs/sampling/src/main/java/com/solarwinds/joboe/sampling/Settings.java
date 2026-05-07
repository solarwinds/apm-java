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

/**
 * Settings that contains information such as sample rate, tracing mode and other arguments
 *
 * @author pluk
 */
public abstract class Settings {
  public static final short OBOE_SETTINGS_FLAG_INVALID = 0x1,
      OBOE_SETTINGS_FLAG_OVERRIDE = 0x2,
      OBOE_SETTINGS_FLAG_SAMPLE_START = 0x4,
      OBOE_SETTINGS_FLAG_SAMPLE_THROUGH = 0x8,
      OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS = 0x10,
      OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED = 0x20,
      OBOE_SETTINGS_FLAG_PROFILING = 0x40;
  public static final short OBOE_SETTINGS_TYPE_SKIP = 0,
      OBOE_SETTINGS_TYPE_STOP = 1,
      OBOE_SETTINGS_TYPE_DEFAULT_SAMPLE_RATE = 2,
      OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE = 3,
      OBOE_SETTINGS_TYPE_LAYER_APP_SAMPLE_RATE = 4, // not used
      OBOE_SETTINGS_TYPE_LAYER_HTTPHOST_SAMPLE_RATE = 5;

  /**
   * Returns value, or null if value is invalid (indicating refresh is required.)
   *
   * @return
   */
  public abstract long getValue();

  public abstract long getTimestamp();

  public abstract short getType();

  public abstract short getFlags();

  public abstract long getTtl();

  public abstract <T> T getArgValue(SettingsArg<T> arg);

  public final boolean isDefault() {
    return (getType() == OBOE_SETTINGS_TYPE_DEFAULT_SAMPLE_RATE);
  }

  @Override
  public String toString() {
    return "[Settings: timestamp="
        + getTimestamp()
        + " type="
        + getType()
        + " flags="
        + getFlags()
        + " value="
        + getValue()
        + " ttl="
        + getTtl()
        + " args="
        + getArgsString()
        + " ]";
  }

  private String getArgsString() {
    StringBuilder builder = new StringBuilder();
    for (SettingsArg<?> arg : SettingsArg.values()) {
      Object value = getArgValue(arg);
      if (value != null) {
        if (arg == SettingsArg.TRACE_OPTIONS_SECRET) {
          builder.append(arg.getKey()).append("=<masked>, ");
        } else {
          builder.append(arg.getKey()).append("=").append(value).append(", ");
        }
      }
    }
    return builder.toString();
  }
}
