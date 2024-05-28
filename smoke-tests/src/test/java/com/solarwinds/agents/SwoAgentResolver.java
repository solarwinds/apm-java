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

package com.solarwinds.agents;

import java.nio.file.Path;
import java.util.Optional;

public class SwoAgentResolver implements AgentResolver {
  private static final String NH_URL = "https://agent-binaries.global.st-ssp.solarwinds.com/apm/java/latest/solarwinds-apm-agent.jar";

  private static final String NH_AGENT_JAR_NAME = "solarwinds-apm-agent.jar";


  public Optional<Path> resolve(Agent agent) {
    return Optional.ofNullable(downloadAgent(NH_URL, NH_AGENT_JAR_NAME));
  }
}
