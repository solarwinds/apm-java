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
