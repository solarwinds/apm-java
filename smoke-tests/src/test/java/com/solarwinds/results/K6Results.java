/*
 * Copyright SolarWinds Worldwide, LLC.
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

package com.solarwinds.results;


import com.solarwinds.agents.Agent;
import com.solarwinds.config.TestConfig;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K6Results {

  Agent agent;
  TestConfig config;
  double iterationAvg;
  double iterationP95;
  double requestAvg;
  double requestP95;
  long totalGCTime;
  long totalAllocated;
  MinMax heapUsed;
  float maxThreadContextSwitchRate;
  long startupDurationMs;
  long peakThreadCount;
  long averageNetworkRead;
  long averageNetworkWrite;
  float averageJvmUserCpu;
  float maxJvmUserCpu;
  float averageMachineCpuTotal;
  long runDurationMs;
  long totalGcPauseNanos;

  public static class MinMax {
    public final long min;
    public final long max;

    public MinMax() {
      this(Long.MAX_VALUE, Long.MIN_VALUE);
    }

    public MinMax(long min, long max) {
      this.min = min;
      this.max = max;
    }

    public MinMax withMin(long min) {
      return new MinMax(min, max);
    }

    public MinMax withMax(long max) {
      return new MinMax(min, max);
    }
  }
}
