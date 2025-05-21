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

package com.solarwinds.opentelemetry.extensions.initialize.config;

import static org.junit.jupiter.api.Assertions.*;

import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.core.profiler.ProfilerSetting;
import java.util.HashSet;
import java.util.Set;

import com.solarwinds.opentelemetry.extensions.config.parser.json.ProfilerSettingParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfilerSettingParserTest {

  @InjectMocks private ProfilerSettingParser tested;

  @Test
  void returnProfilerSettingGivenValidJson() throws InvalidConfigException {
    String json =
        "{"
            + "\"enabled\": \"true\","
            + "\"interval\": 45,"
            + "\"circuitBreakerDurationThreshold\": 4,"
            + "\"circuitBreakerCountThreshold\": 5,"
            + "\"excludePackages\":[\"com.cleverchuk\", \"org.qwerty\"],"
            + "}";

    Set<String> expectedExcludes = new HashSet<>();
    expectedExcludes.add("com.cleverchuk");
    expectedExcludes.add("org.qwerty");

    ProfilerSetting actual = tested.convert(json);

    assertTrue(actual.isEnabled());
    assertEquals(45, actual.getInterval());
    assertEquals(4, actual.getCircuitBreakerDurationThreshold());

    assertEquals(5, actual.getCircuitBreakerCountThreshold());
    assertEquals(expectedExcludes, actual.getExcludePackages());
  }

  @Test
  void throwInvalidConfigExceptionGivenValidJsonWithWrongType() throws InvalidConfigException {
    String json =
        "{"
            + "\"enabled\": \"true\","
            + "\"interval\": \"this fails\","
            + "\"circuitBreakerDurationThreshold\": 4,"
            + "\"circuitBreakerCountThreshold\": 5,"
            + "\"excludePackages\":[\"com.cleverchuk\", \"org.qwerty\"],"
            + "}";
    assertThrows(InvalidConfigException.class, () -> tested.convert(json));
  }

  @Test
  void throwInvalidConfigExceptionGivenValidJsonWithWrongLowInterval()
      throws InvalidConfigException {
    String json =
        "{"
            + "\"enabled\": \"true\","
            + "\"interval\": 4,"
            + "\"circuitBreakerDurationThreshold\": 4,"
            + "\"circuitBreakerCountThreshold\": 5,"
            + "\"excludePackages\":[\"com.cleverchuk\", \"org.qwerty\"],"
            + "}";
    assertThrows(InvalidConfigException.class, () -> tested.convert(json));
  }
}
