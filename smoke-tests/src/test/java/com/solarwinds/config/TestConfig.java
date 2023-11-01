/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.solarwinds.config;

import com.solarwinds.agents.Agent;
import lombok.Builder;

import java.util.List;

/**
 * Defines a test config.
 */
@Builder
public record TestConfig(String name, String description, List<Agent> agents, int maxRequestRate,
                         int concurrentConnections, int totalIterations, int warmupSeconds) {

  private static final int DEFAULT_MAX_REQUEST_RATE = 0; // none
  private static final int DEFAULT_CONCURRENT_CONNECTIONS = 5;
  private static final int DEFAULT_TOTAL_ITERATIONS = 5000;

}