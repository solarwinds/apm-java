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

package com.solarwinds.opentelemetry.extensions.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.solarwinds.joboe.sampling.Settings;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class JsonSettingWrapperTest {

  @Test
  void getFlagsParsesProfilingToken() {
    JsonSetting jsonSetting =
        JsonSetting.builder().flags("PROFILING").arguments(Collections.emptyMap()).build();

    JsonSettingWrapper wrapper = new JsonSettingWrapper(jsonSetting);
    assertEquals(Settings.OBOE_SETTINGS_FLAG_PROFILING, wrapper.getFlags());
  }

  @Test
  void getFlagsParsesProfilingWithOtherTokens() {
    JsonSetting jsonSetting =
        JsonSetting.builder()
            .flags("OVERRIDE,SAMPLE_START,PROFILING,TRIGGER_TRACE")
            .arguments(Collections.emptyMap())
            .build();

    JsonSettingWrapper wrapper = new JsonSettingWrapper(jsonSetting);
    short expected =
        (short)
            (Settings.OBOE_SETTINGS_FLAG_OVERRIDE
                | Settings.OBOE_SETTINGS_FLAG_SAMPLE_START
                | Settings.OBOE_SETTINGS_FLAG_PROFILING
                | Settings.OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED);
    assertEquals(expected, wrapper.getFlags());
  }

  @Test
  void getFlagsWithoutProfilingDoesNotSetBit() {
    JsonSetting jsonSetting =
        JsonSetting.builder()
            .flags("OVERRIDE,SAMPLE_START,TRIGGER_TRACE")
            .arguments(Collections.emptyMap())
            .build();

    JsonSettingWrapper wrapper = new JsonSettingWrapper(jsonSetting);
    assertEquals(
        0,
        wrapper.getFlags() & Settings.OBOE_SETTINGS_FLAG_PROFILING,
        "PROFILING bit should not be set");
  }
}
