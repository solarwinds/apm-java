package com.solarwinds;

import com.solarwinds.agents.Agent;
import com.solarwinds.agents.SwoAgentResolver;
import com.solarwinds.config.Configs;
import com.solarwinds.config.TestConfig;
import com.solarwinds.containers.K6Container;
import com.solarwinds.containers.PetClinicRestContainer;
import com.solarwinds.containers.PostgresContainer;
import com.solarwinds.results.ResultsCollector;
import com.solarwinds.util.NamingConventions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class SmokeTest {
    private static final Network NETWORK = Network.newNetwork();

    private static final NamingConventions namingConventions = new NamingConventions();


    @BeforeAll
    static void runTestConfig() {
        TestConfig config = Configs.E2E.config;
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

        GenericContainer<?> petClinic = new PetClinicRestContainer(new SwoAgentResolver(), NETWORK, agent, namingConventions).build();
        petClinic.start();
        petClinic.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger("k6")), OutputFrame.OutputType.STDOUT);

        GenericContainer<?> k6 = new K6Container(NETWORK, agent, config, namingConventions).build();
        k6.start();
        k6.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger("k6")), OutputFrame.OutputType.STDOUT);

        petClinic.execInContainer("kill", "1");
        postgres.stop();

    }

    @Test
    void assertXTrace() throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double fail = ResultsCollector.read(resultJson, "$.root_group.checks.['should have X-Trace header'].fails");
        assertEquals(0, fail, "Less than a 100 percent of the responses has X-Trace header");
    }

    @Test
    void assertTransactionFiltering() throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double fail = ResultsCollector.read(resultJson, "$.root_group.checks.['verify that transaction is filtered'].fails");
        assertEquals(0, fail, "transaction filtering doesn't work");
    }

    @Test
    void assertTraceIngestion() throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double pass = ResultsCollector.read(resultJson, "$.root_group.checks.['trace is returned'].passes");
        assertTrue(pass > 0, "trace ingestion is not working. There maybe network issues");
    }

    @Test
    void assertJDBC()  throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double pass = ResultsCollector.read(resultJson, "$.root_group.checks.['JDBC is not broken'].passes");
        assertTrue(pass > 0, "JDBC instrumentation doesn't work");
    }

    @Test
    void assertXTraceOptions()  throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double pass = ResultsCollector.read(resultJson, "$.root_group.checks.['xtrace-options is added to root span'].passes");
        assertTrue(pass > 2, "Xtrace options is not captured in root span");
    }

    @Test
    void assertMvcInstrumentation()  throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double pass = ResultsCollector.read(resultJson, "$.root_group.checks.['mvc handler name is added'].passes");
        assertTrue(pass > 0, "MVC instrumentation is broken");
    }

    @Test
    void assertTriggerTrace()  throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double pass = ResultsCollector.read(resultJson, "$.root_group.checks.['trigger trace'].passes");
        assertTrue(pass > 0, "trigger trace is broken");
    }

}
