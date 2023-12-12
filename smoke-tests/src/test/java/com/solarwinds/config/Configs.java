/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.solarwinds.config;


import com.solarwinds.agents.Agent;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

/**
 * Defines all test configurations
 */
public enum Configs {
  E2E(
      TestConfig.builder()
          .name("E2E")
          .description("Smoke tests java apm agent end-to-end with the backend")
          .agents(Collections.singletonList(Agent.SWO_JAVA_AGENT))
          .concurrentConnections(2)
          .totalIterations(100)
          .warmupSeconds(60)
          .build()),
  LAMBDA_E2E(
      TestConfig.builder()
          .name("LAMBDA_E2E")
          .description("Smoke tests java apm agent end-to-end with the backend")
          .agents(Collections.singletonList(Agent.SWO_JAVA_AGENT))
          .concurrentConnections(1)
          .totalIterations(5)
          .warmupSeconds(60)
          .build());

  public final TestConfig config;

  public static Stream<TestConfig> all() {
    return Arrays.stream(Configs.values()).map(x -> x.config);
  }

  Configs(TestConfig config) {
    this.config = config;
  }
}
