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
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.nio.file.Files;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class SmokeTest {
    private static final Network NETWORK = Network.newNetwork();

    private final NamingConventions namingConventions = new NamingConventions();

    @TestFactory
    Stream<DynamicTest> runAllTestConfigurations() {
        return Configs.all().map(config -> dynamicTest(config.name(), () -> runTestConfig(config)));
    }

    void runTestConfig(TestConfig config) {
        config
                .agents()
                .forEach(
                        agent -> {
                            try {
                                String resultJson = runAppOnce(config, agent);
                                assertXTrace(resultJson);
                            } catch (Exception e) {
                                fail("Unhandled exception in " + config.name(), e);
                            }
                        });
    }

    String runAppOnce(TestConfig config, Agent agent) throws Exception {
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

        return new String(Files.readAllBytes(namingConventions.local.k6Results(agent)));
    }

    void assertXTrace(String resultJson){
        double fail = ResultsCollector.read(resultJson, "$.root_group.checks.['should have X-Trace header'].fails");
        assertEquals(0, fail,"verify that 100 percent of the responses has X-Trace header");
    }

}
