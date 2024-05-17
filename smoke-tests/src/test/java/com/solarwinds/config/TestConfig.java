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
