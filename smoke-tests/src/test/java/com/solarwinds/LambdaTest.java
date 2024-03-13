package com.solarwinds;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.solarwinds.agents.Agent;
import com.solarwinds.agents.SwoAgentResolver;
import com.solarwinds.config.Configs;
import com.solarwinds.config.TestConfig;
import com.solarwinds.containers.K6Container;
import com.solarwinds.containers.PetClinicRestContainer;
import com.solarwinds.containers.PostgresContainer;
import com.solarwinds.results.ResultsCollector;
import com.solarwinds.util.LogStreamAnalyzer;
import com.solarwinds.util.NamingConventions;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;

@EnabledIfEnvironmentVariable(named = "LAMBDA", matches = "true")
public class LambdaTest {
  private static final Network NETWORK = Network.newNetwork();

  private static final NamingConventions namingConventions = new NamingConventions();

  private static final LogStreamAnalyzer<Slf4jLogConsumer> logStreamAnalyzer = new LogStreamAnalyzer<>(
      List.of("Got settings from file:")
      , new Slf4jLogConsumer(LoggerFactory.getLogger("k6")));


  @BeforeAll
  static void runTestConfig() {
    TestConfig config = Configs.LAMBDA_E2E.config;
    config
        .agents()
        .forEach(
            agent -> {
              try {
                runAppOnce(config, agent);
              } catch (Exception e) {
                fail("Unhandled exception in " + config.name(), e);
              }
            });
  }

  static void runAppOnce(TestConfig config, Agent agent) throws Exception {
    GenericContainer<?> postgres = new PostgresContainer(NETWORK).build();
    postgres.start();

    GenericContainer<?> petClinic = new PetClinicRestContainer(new SwoAgentResolver(), NETWORK,
        agent, namingConventions).build();
    petClinic.start();
    petClinic.followOutput(logStreamAnalyzer, OutputFrame.OutputType.STDOUT);

    GenericContainer<?> k6 = new K6Container(NETWORK, agent, config, namingConventions).build();
    k6.start();
    k6.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger("k6")),
        OutputFrame.OutputType.STDOUT);

    petClinic.execInContainer("kill", "1");
    postgres.stop();
  }

  @Test
  void assertThatRequestCountMetricIsReported() throws IOException {
    String resultJson = new String(
        Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));

    double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['request_count'].passes");
    assertTrue(passes > 1, "Expects a count > 1 ");
  }

  @Test
  void assertThatTraceCountMetricIsReported() throws IOException {
    String resultJson = new String(
        Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));

    double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['tracecount'].passes");
    assertTrue(passes > 1, "Expects a count > 1 ");
  }

  @Test
  void assertThatSampleCountMetricIsReported() throws IOException {
    String resultJson = new String(
        Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));

    double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['samplecount'].passes");
    assertTrue(passes > 1, "Expects a count > 1 ");
  }

  @Test
  void assertThatResponseTimeMetricIsReported() throws IOException {
    String resultJson = new String(
        Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));

    double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['response_time'].passes");
    assertTrue(passes > 1, "Expects a count > 1 ");
  }

  @Test
  void assertThatCustomTransactionNameTakesEffect() throws IOException {
    String resultJson = new String(
        Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));

    double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['transaction-name'].passes");
    assertTrue(passes > 1, "Expects a count > 1 ");
  }

  @Test
  void assertThatSettingsAreReadFromFile() {
    Boolean actual = logStreamAnalyzer.getAnswer().get("Got settings from file:");
    assertTrue(actual, "file based settings is not being used");
  }
}
