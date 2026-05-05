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

package com.solarwinds.joboe.core.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.solarwinds.joboe.sampling.Settings;
import org.junit.jupiter.api.Test;

class RpcSettingsTest {

  @Test
  void convertFlagsFromStringToShortParsesProfiling() {
    short flags = RpcSettings.convertFlagsFromStringToShort("PROFILING");
    assertEquals(Settings.OBOE_SETTINGS_FLAG_PROFILING, flags);
  }

  @Test
  void convertFlagsFromStringToShortParsesProfilingWithOtherFlags() {
    short flags = RpcSettings.convertFlagsFromStringToShort("OVERRIDE,SAMPLE_START,PROFILING");
    short expected =
        (short)
            (Settings.OBOE_SETTINGS_FLAG_OVERRIDE
                | Settings.OBOE_SETTINGS_FLAG_SAMPLE_START
                | Settings.OBOE_SETTINGS_FLAG_PROFILING);
    assertEquals(expected, flags);
  }

  @Test
  void convertFlagsFromStringToShortParsesAllKnownFlags() {
    short flags =
        RpcSettings.convertFlagsFromStringToShort(
            "OVERRIDE,SAMPLE_START,SAMPLE_THROUGH,SAMPLE_THROUGH_ALWAYS,TRIGGER_TRACE,PROFILING");
    short expected =
        (short)
            (Settings.OBOE_SETTINGS_FLAG_OVERRIDE
                | Settings.OBOE_SETTINGS_FLAG_SAMPLE_START
                | Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH
                | Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS
                | Settings.OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED
                | Settings.OBOE_SETTINGS_FLAG_PROFILING);
    assertEquals(expected, flags);
  }

  @Test
  void convertFlagsFromStringToShortWithoutProfilingDoesNotSetBit() {
    short flags = RpcSettings.convertFlagsFromStringToShort("OVERRIDE,SAMPLE_START,TRIGGER_TRACE");
    assertEquals(
        0, flags & Settings.OBOE_SETTINGS_FLAG_PROFILING, "PROFILING bit should not be set");
  }

  @Test
  void convertFlagsFromStringToShortIgnoresUnknownFlags() {
    short flags = RpcSettings.convertFlagsFromStringToShort("PROFILING,UNKNOWN_FLAG");
    assertEquals(Settings.OBOE_SETTINGS_FLAG_PROFILING, flags);
  }

  @Test
  void convertFlagsFromStringToShortEmptyStringReturnsZero() {
    short flags = RpcSettings.convertFlagsFromStringToShort("");
    assertEquals(0, flags);
  }
}
