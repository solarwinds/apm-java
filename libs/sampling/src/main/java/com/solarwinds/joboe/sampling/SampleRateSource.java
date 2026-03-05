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
 * Matches oboe C library OBOE_SAMPLE_RATE_SOURCE values
 *
 * <p>See oboe_inst_macros.h
 */
public enum SampleRateSource {
  FILE(1), // locally configured rate, could be from file (agent.sampleRate or url patterns) or JVM
  // args
  DEFAULT(2),
  OBOE(3),
  LAST_OBOE(4),
  DEFAULT_MISCONFIGURED(5),
  OBOE_DEFAULT(6);

  private final int value;

  SampleRateSource(int value) {
    this.value = value;
  }

  public int value() {
    return value;
  }
}
