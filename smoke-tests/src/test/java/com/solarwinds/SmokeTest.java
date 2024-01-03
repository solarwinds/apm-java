package com.solarwinds;

import com.solarwinds.agents.Agent;
import com.solarwinds.agents.SwoAgentResolver;
import com.solarwinds.config.Configs;
import com.solarwinds.config.TestConfig;
import com.solarwinds.containers.AoContainer;
import com.solarwinds.containers.K6Container;
import com.solarwinds.containers.PetClinicRestContainer;
import com.solarwinds.containers.PostgresContainer;
import com.solarwinds.containers.SpringBootWebMvcContainer;
import com.solarwinds.results.ResultsCollector;
import com.solarwinds.util.LogStreamAnalyzer;
import com.solarwinds.util.NamingConventions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class SmokeTest {
    private static final Network NETWORK = Network.newNetwork();

    private static final NamingConventions namingConventions = new NamingConventions();

    private static final LogStreamAnalyzer<Slf4jLogConsumer> logStreamAnalyzer = new LogStreamAnalyzer<>(
            List.of("Transformed (com.appoptics.ext.*|com.solarwinds.joboe.*)","hostId:.*i-[0-9a-z]+",
                    "Completed operation \\[post init message\\] with Result code \\[OK\\] arg",
                    "hostId:.*[0-9a-z-]+", "Extension attached!","Created collector client  : collector.appoptics.com:443",
                    "trace_id=[a-z0-9]+\\s+span_id=[a-z0-9]+\\s+trace_flags=(01|00)",
                    "This log line is used for validation only: service.name: java-apm-smoke-test",
                    "Clearing transaction name buffer. Unique transaction count: \\d+")
            , new Slf4jLogConsumer(LoggerFactory.getLogger("k6")));


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
        GenericContainer<?> webMvc = new SpringBootWebMvcContainer(new SwoAgentResolver(), NETWORK, agent).build();
        webMvc.start();
        webMvc.followOutput(logStreamAnalyzer, OutputFrame.OutputType.STDOUT);

        GenericContainer<?> webMvcAo = new AoContainer(new SwoAgentResolver(), NETWORK, agent).build();
        webMvcAo.start();
        webMvcAo.followOutput(logStreamAnalyzer, OutputFrame.OutputType.STDOUT);

        GenericContainer<?> postgres = new PostgresContainer(NETWORK).build();
        postgres.start();

        GenericContainer<?> petClinic = new PetClinicRestContainer(new SwoAgentResolver(), NETWORK, agent, namingConventions).build();
        petClinic.start();
        petClinic.followOutput(logStreamAnalyzer, OutputFrame.OutputType.STDOUT);

        GenericContainer<?> k6 = new K6Container(NETWORK, agent, config, namingConventions).build();
        k6.start();
        k6.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger("k6")), OutputFrame.OutputType.STDOUT);

        petClinic.execInContainer("kill", "1");
        webMvc.execInContainer("kill", "1");
        webMvcAo.execInContainer("kill", "1");
        postgres.stop();

    }

    @Test
    void assertXTrace() throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double fails = ResultsCollector.read(resultJson, "$.root_group.checks.['should have X-Trace header'].fails");
        assertEquals(0, fails, "Less than a 100 percent of the responses has X-Trace header");
    }

    @Test
    void assertTransactionFiltering() throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double fails = ResultsCollector.read(resultJson, "$.root_group.checks.['verify that transaction is filtered'].fails");
        assertEquals(0, fails, "transaction filtering doesn't work");
    }

    @Test
    void assertTraceIngestion() throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['trace is returned'].passes");
        assertTrue(passes > 0, "trace ingestion is not working. There maybe network issues");
    }

    @Test
    void assertJDBC()  throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['JDBC is not broken'].passes");
        assertTrue(passes > 0, "JDBC instrumentation doesn't work");
    }

    @Test
    void assertXTraceOptions()  throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['xtrace-options is added to root span'].passes");
        assertTrue(passes > 2, "Xtrace options is not captured in root span");
    }

    @Test
    void assertMvcInstrumentation()  throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['mvc handler name is added'].passes");
        assertTrue(passes > 0, "MVC instrumentation is broken");
    }

    @Test
    void assertTriggerTrace()  throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['trigger trace'].passes");
        assertTrue(passes > 0, "trigger trace is broken");
    }
    
    @Test
    @Disabled
    void assertCodeProfiling()  throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['code profiling'].passes");
        assertTrue(passes > 0, "code profiling is broken");
    }

    @Test
    void assertContextPropagation()  throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['check that remote service, java-apm-smoke-test, is path of the trace'].passes");
        assertTrue(passes > 0, "context propagation is broken");
    }

    @Test
    void assertConnectionToAo() {
        Boolean actual = logStreamAnalyzer.getAnswer().get("Created collector client  : collector.appoptics.com:443");
        assertTrue(actual, "connectivity to appoptics is broken");
    }

    @Test
    void assertTransactionNaming() throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['custom transaction name'].passes");
        assertTrue(passes > 1, "transaction naming is broken");
    }


    @Test
    void assertAgentClassesAreNotInstrumented() {
        Boolean actual = logStreamAnalyzer.getAnswer().get("Transformed (com.appoptics.ext.*|com.solarwinds.joboe.*)");
        assertFalse(actual, "agent classes are instrumented");
    }

    @Test
    void assertTraceContextInLog() {
        Boolean actual = logStreamAnalyzer.getAnswer().get("trace_id=[a-z0-9]+\\s+span_id=[a-z0-9]+\\s+trace_flags=(01|00)");
        assertTrue(actual, "trace context is not injected in logs");
    }

    @Test
    void assertAgentExtensionLoading() {
        Boolean actual = logStreamAnalyzer.getAnswer().get("Extension attached!");
        assertTrue(actual, "expected log output from extension was not found");
    }

    @Test
    void assertInitMessageIsSent() {
        Boolean actual = logStreamAnalyzer.getAnswer().get("Completed operation \\[post init message\\] with Result code \\[OK\\] arg");
        assertTrue(actual, "init message wasn't sent");
    }


    @Test
    @EnabledIfSystemProperty(named = "test.cloud", matches = "AWS")
    void assertAgentAwsMetadata() {
        Boolean actual = logStreamAnalyzer.getAnswer().get("hostId:.*i-[0-9a-z]+");
        assertTrue(actual, "AWS metadata is not retrieved");
    }

    @Test
    @EnabledIfSystemProperty(named = "test.cloud", matches = "AZURE")
    void assertAgentAzureMetadata() {
        Boolean actual = logStreamAnalyzer.getAnswer().get("hostId:.*[0-9a-z-]+");
        assertTrue(actual, "Azure metadata is not retrieved");
    }

    @Test
    void assertServiceNameIsSameAsOneInServiceKey() {
        Boolean actual = logStreamAnalyzer.getAnswer().get("This log line is used for validation only: service.name: java-apm-smoke-test");
        assertTrue(actual, "service.name is not updated with name in service key");
    }

    @Test
    void assertThatTransactionNameBufferIsCleared() {
        Boolean actual = logStreamAnalyzer.getAnswer().get("Clearing transaction name buffer. Unique transaction count: \\d+");
        assertTrue(actual, "Transaction name buffer is not getting cleared on metric flush");
    }

}
