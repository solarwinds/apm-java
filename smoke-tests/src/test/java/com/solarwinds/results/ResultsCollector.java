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

package com.solarwinds.results;

import com.jayway.jsonpath.JsonPath;
import com.solarwinds.agents.Agent;
import com.solarwinds.config.TestConfig;
import com.solarwinds.util.NamingConvention;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ResultsCollector {

  private final NamingConvention namingConvention;

  public ResultsCollector(NamingConvention namingConvention) {
    this.namingConvention = namingConvention;
  }

  public List<K6Results> collect(TestConfig config) {
    return config.agents().stream()
        .map(a -> readAgentResults(a, config))
        .collect(Collectors.toList());
  }

  private K6Results readAgentResults(Agent agent, TestConfig config) {
    try {
      K6Results.K6ResultsBuilder builder =
          K6Results.builder()
              .agent(agent)
              .config(config);

      builder = addK6Results(builder, agent);

      return builder.build();
    } catch (IOException e) {
      throw new RuntimeException("Error reading results", e);
    }
  }


  private K6Results.K6ResultsBuilder addK6Results(K6Results.K6ResultsBuilder builder, Agent agent)
      throws IOException {
    Path k6File = namingConvention.k6Results(agent);
    String json = new String(Files.readAllBytes(k6File));
    double iterationAvg = read(json, "$.metrics.iteration_duration.avg");
    double iterationP95 = read(json, "$.metrics.iteration_duration['p(95)']");
    double requestAvg = read(json, "$.metrics.http_req_duration.avg");
    double requestP95 = read(json, "$.metrics.http_req_duration['p(95)']");
    return builder
        .iterationAvg(iterationAvg)
        .iterationP95(iterationP95)
        .requestAvg(requestAvg)
        .requestP95(requestP95);
  }

  public static double read(String json, String jsonPath) {
    Number result = JsonPath.read(json, jsonPath);
    return result.doubleValue();
  }
}
