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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TraceConfigTest {

  @Test
  void hasProfilingFlagReturnsTrueWhenSet() {
    TraceConfig config =
        new TraceConfig(
            1000000, SampleRateSource.OBOE_DEFAULT, Settings.OBOE_SETTINGS_FLAG_PROFILING);
    assertTrue(config.hasProfilingFlag());
  }

  @Test
  void hasProfilingFlagReturnsTrueWhenSetWithOtherFlags() {
    short flags =
        (short)
            (Settings.OBOE_SETTINGS_FLAG_SAMPLE_START
                | Settings.OBOE_SETTINGS_FLAG_OVERRIDE
                | Settings.OBOE_SETTINGS_FLAG_PROFILING);
    TraceConfig config = new TraceConfig(1000000, SampleRateSource.OBOE_DEFAULT, flags);
    assertTrue(config.hasProfilingFlag());
  }

  @Test
  void hasProfilingFlagReturnsFalseWhenNotSet() {
    short flags =
        (short)
            (Settings.OBOE_SETTINGS_FLAG_SAMPLE_START
                | Settings.OBOE_SETTINGS_FLAG_OVERRIDE
                | Settings.OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED);
    TraceConfig config = new TraceConfig(1000000, SampleRateSource.OBOE_DEFAULT, flags);
    assertFalse(config.hasProfilingFlag());
  }

  @Test
  void hasProfilingFlagReturnsFalseWhenFlagsNull() {
    TraceConfig config = new TraceConfig(1000000, SampleRateSource.OBOE_DEFAULT, null);
    assertFalse(config.hasProfilingFlag());
  }

  @Test
  void hasProfilingFlagReturnsFalseWhenFlagsZero() {
    TraceConfig config = new TraceConfig(1000000, SampleRateSource.OBOE_DEFAULT, (short) 0);
    assertFalse(config.hasProfilingFlag());
  }
}
