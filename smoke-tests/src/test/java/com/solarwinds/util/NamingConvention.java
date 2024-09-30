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

package com.solarwinds.util;

import com.solarwinds.agents.Agent;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This utility class provides the standard file naming conventions, primarily for files that are
 * shared between containers and the test runner. It consolidates the naming logic into one place to
 * ensure consistency, reduce duplication, and decrease errors.
 */
public class NamingConvention {

  private final String dir;

  public NamingConvention(String dir) {
    this.dir = dir;
  }

  /**
   * Returns a path to the location of the k6 results json file.
   *
   * @param agent The agent to get results file path for
   */
  public Path k6Results(Agent agent) {
    return Paths.get(dir, "k6_out_" + agent.getName() + ".json");
  }

  /** Returns the root path that this naming convention was configured with. */
  public String root() {
    return dir;
  }
}
