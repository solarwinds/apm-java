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

import static com.solarwinds.joboe.sampling.Settings.OBOE_SETTINGS_FLAG_SAMPLE_START;
import static com.solarwinds.joboe.sampling.Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS;
import static com.solarwinds.joboe.sampling.Settings.OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED;

import lombok.Getter;

@Getter
public enum TracingMode {
  ALWAYS("always"), // deprecated
  ENABLED("enabled"),
  NEVER("never"), // deprecated
  DISABLED("disabled");

  private final String stringValue;

  TracingMode(String stringValue) {
    this.stringValue = stringValue;
  }

  public static TracingMode fromString(String stringValue) {
    for (TracingMode mode : values()) {
      if (mode.stringValue.equals(stringValue)) {
        return mode;
      }
    }

    return null;
  }

  // convert agent tracing mode to settings flags
  // XXX: Using THROUGH_ALWAYS to maintain previous behaviour when setting sample rate from command
  // line/config
  public short toFlags() {
    switch (this) {
      case ALWAYS:
      case ENABLED:
        return OBOE_SETTINGS_FLAG_SAMPLE_START
            | OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS
            | OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED;

      case NEVER:
      case DISABLED:
      default:
        return 0x00;
    }
  }
}
