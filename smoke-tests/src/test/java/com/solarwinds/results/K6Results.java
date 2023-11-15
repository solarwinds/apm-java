/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
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
