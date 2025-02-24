/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.config;

import io.opentelemetry.agents.Agent;
import java.util.Arrays;
import java.util.stream.Stream;

/** Defines all test configurations */
public enum Configs {
  RELEASE(
      TestConfig.builder()
          .name("release")
          .description("compares no agent, latest Otel standard, and latest SWO agents")
          .withAgents(
              Agent.OTEL_LATEST_RELEASE,
              Agent.SWO_SNAPSHOT_RELEASE,
              Agent.SWO_GA_RELEASE,
              Agent.NONE)
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
