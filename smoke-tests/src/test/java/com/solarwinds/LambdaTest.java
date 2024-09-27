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

package com.solarwinds;

import com.solarwinds.agents.Agent;
import com.solarwinds.agents.SwoLambdaAgentResolver;
import com.solarwinds.config.Configs;
import com.solarwinds.config.TestConfig;
import com.solarwinds.containers.K6Container;
import com.solarwinds.containers.PetClinicRestContainer;
import com.solarwinds.containers.PostgresContainer;
import com.solarwinds.containers.SpringBootWebMvcContainer;
import com.solarwinds.results.ResultsCollector;
import com.solarwinds.util.LogStreamAnalyzer;
import com.solarwinds.util.NamingConventions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "LAMBDA", matches = "true")
public class LambdaTest {
  private static final Network NETWORK = Network.newNetwork();

  private static final NamingConventions namingConventions = new NamingConventions();

  private static final LogStreamAnalyzer<Slf4jLogConsumer> logStreamAnalyzer = new LogStreamAnalyzer<>(
      List.of(
          "Got settings from file:",
          "Applying instrumentation: sw-jdbc"
      )
      , new Slf4jLogConsumer(LoggerFactory.getLogger("k6")));


  @BeforeAll
  static void runTestConfig() {
    TestConfig config = Configs.LAMBDA_E2E.config;
    config
        .agents()
        .forEach(
            agent -> {
              try {
                runAppOnce(agent);
              } catch (Exception e) {
                fail("Unhandled exception in " + config.name(), e);
              }
            });
  }

  static void runAppOnce(Agent agent) throws Exception {
    GenericContainer<?> postgres = new PostgresContainer(NETWORK).build();
    postgres.start();

    GenericContainer<?> webMvc = new SpringBootWebMvcContainer(new SwoLambdaAgentResolver(), NETWORK, agent).build();
    webMvc.start();

    GenericContainer<?> petClinic = new PetClinicRestContainer(new SwoLambdaAgentResolver(), NETWORK, agent).build();
    petClinic.start();
    petClinic.followOutput(logStreamAnalyzer);

    GenericContainer<?> k6 = new K6Container(NETWORK, agent, namingConventions).build();
    k6.start();
    k6.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger("k6")));

    petClinic.execInContainer("kill", "1");
    webMvc.execInContainer("kill", "1");
    postgres.stop();
  }

  @Test
  void assertThatRequestCountMetricIsReported() throws IOException {
    String resultJson = new String(
            Files.readAllBytes(namingConventions.local.k6Results(Configs.LAMBDA_E2E.config.agents().get(0))));

    double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['request_count'].passes");
    assertTrue(passes > 1, "Expects a count > 1 ");
  }

  @Test
  void assertThatTraceCountMetricIsReported() throws IOException {
    String resultJson = new String(
            Files.readAllBytes(namingConventions.local.k6Results(Configs.LAMBDA_E2E.config.agents().get(0))));

    double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['tracecount'].passes");
    assertTrue(passes > 1, "Expects a count > 1 ");
  }

  @Test
  void assertThatSampleCountMetricIsReported() throws IOException {
    String resultJson = new String(
            Files.readAllBytes(namingConventions.local.k6Results(Configs.LAMBDA_E2E.config.agents().get(0))));

    double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['samplecount'].passes");
    assertTrue(passes > 1, "Expects a count > 1 ");
  }

  @Test
  void assertThatResponseTimeMetricIsReported() throws IOException {
    String resultJson = new String(
            Files.readAllBytes(namingConventions.local.k6Results(Configs.LAMBDA_E2E.config.agents().get(0))));

    double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['response_time'].passes");
    assertTrue(passes > 1, "Expects a count > 1 ");
  }

  @Test
  void assertThatCustomTransactionNameTakesEffect() throws IOException {
    String resultJson = new String(
            Files.readAllBytes(namingConventions.local.k6Results(Configs.LAMBDA_E2E.config.agents().get(0))));

    double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['transaction-name'].passes");
    assertTrue(passes > 1, "Environment based transaction naming is broken ");
  }

  @Test
  void assertThatSettingsAreReadFromFile() {
    Boolean actual = logStreamAnalyzer.getAnswer().get("Got settings from file:");
    assertTrue(actual, "file based settings is not being used");
  }

  @Test
  void assertThatJDBCInstrumentationIsApplied() {
    Boolean actual = logStreamAnalyzer.getAnswer().get("Applying instrumentation: sw-jdbc");
    assertTrue(actual, "sw-jdbc instrumentation is not applied");
  }

  @Test
  void assertSDKTransactionNaming() throws IOException {
    String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.LAMBDA_E2E.config.agents().get(0))));
    double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['custom transaction name'].passes");
    assertTrue(passes > 1, "SDK transaction naming is broken");
  }
}
