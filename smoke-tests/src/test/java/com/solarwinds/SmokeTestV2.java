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
import com.solarwinds.agents.SwoAgentResolver;
import com.solarwinds.config.Configs;
import com.solarwinds.config.TestConfig;
import com.solarwinds.containers.ExtensionContainer;
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
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "SMOKEV2", matches = "true")
public class SmokeTestV2 {
    private static final Network NETWORK = Network.newNetwork();

    private static final NamingConventions namingConventions = new NamingConventions();

    private static final LogStreamAnalyzer<Slf4jLogConsumer> logStreamAnalyzer = new LogStreamAnalyzer<>(
            List.of("hostId:.*i-[0-9a-z]+",
                    "hostId:.*[0-9a-z-]+",
                    "Extension attached!",
                    "Completed operation \\[post init message\\] with Result code \\[OK\\] arg",
                    "trace_id=[a-z0-9]+\\s+span_id=[a-z0-9]+\\s+trace_flags=(01|00)",
                    "This log line is used for validation only: service.name: java-apm-smoke-test",
                    "Applying instrumentation: sw-jdbc",
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
                                runAppOnce(agent);
                            } catch (Exception e) {
                                fail("Unhandled exception in " + config.name(), e);
                            }
                        });
    }

    static void runAppOnce(Agent agent) throws Exception {
        GenericContainer<?> webMvc = new SpringBootWebMvcContainer(new SwoAgentResolver(), NETWORK, agent).build();
        webMvc.start();
        webMvc.followOutput(logStreamAnalyzer);

        GenericContainer<?> webMvcAo = new ExtensionContainer(new SwoAgentResolver(), NETWORK, agent).build();
        webMvcAo.start();
        webMvcAo.followOutput(logStreamAnalyzer);

        GenericContainer<?> postgres = new PostgresContainer(NETWORK).build();
        postgres.start();

        GenericContainer<?> petClinic = new PetClinicRestContainer(new SwoAgentResolver(), NETWORK, agent).build();
        petClinic.start();
        petClinic.followOutput(logStreamAnalyzer);

        GenericContainer<?> k6 = new K6Container(NETWORK, agent, namingConventions).build();
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
    void assertJDBC() throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['JDBC is not broken'].passes");
        assertTrue(passes > 0, "JDBC instrumentation doesn't work");
    }

    @Test
    void assertXTraceOptions() throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['xtrace-options is added to root span'].passes");
        assertTrue(passes > 1, "Xtrace options is not captured in root span");
    }

    @Test
    void assertMvcInstrumentation() throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['mvc handler name is added'].passes");
        assertTrue(passes > 0, "MVC instrumentation is broken");
    }

    @Test
    void assertTriggerTrace() throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['trigger trace'].passes");
        assertTrue(passes > 0, "trigger trace is broken");
    }

    @Test
    @Disabled
    void assertCodeProfiling() throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['code profiling'].passes");
        assertTrue(passes > 0, "code profiling is broken");
    }

    @Test
    void assertContextPropagation() throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['check that remote service, java-apm-smoke-test, is path of the trace'].passes");
        assertTrue(passes > 0, "context propagation is broken");
    }

    @Test
    void assertTransactionNaming() throws IOException {
        String resultJson = new String(Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['custom transaction name'].passes");
        assertTrue(passes > 0, "transaction naming is broken");
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
    void assertThatTransactionNameBufferIsCleared() {
        Boolean actual = logStreamAnalyzer.getAnswer().get("Clearing transaction name buffer. Unique transaction count: \\d+");
        assertTrue(actual, "Transaction name buffer is not getting cleared on metric flush");
    }

    @Test
    void assertThatJDBCInstrumentationIsApplied() {
        Boolean actual = logStreamAnalyzer.getAnswer().get("Applying instrumentation: sw-jdbc");
        assertTrue(actual, "sw-jdbc instrumentation is not applied");
    }

    @Test
    void assertThatLogsAreExported() throws IOException {
        String resultJson = new String(
                Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double passes = ResultsCollector.read(resultJson,
                "$.root_group.checks.['logs'].passes");
        assertTrue(passes > 0, "log export is broken");
    }

    @Test
    void assertThatMetricsAreExported() throws IOException {
        String resultJson = new String(
                Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));
        double passes = ResultsCollector.read(resultJson,
                "$.root_group.checks.['otel-metrics'].passes");
        assertTrue(passes > 0, "otel metric export is broken");
    }

    @Test
    void assertThatSdkTracingIsWorking() throws IOException {
        String resultJson = new String(
                Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));

        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['sdk-trace'].passes");
        assertTrue(passes > 0, "SDK trace is not working, expected a count > 0");
    }

    @Test
    void assertThatRequestCountMetricIsReported() throws IOException {
        String resultJson = new String(
                Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));

        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['request_count'].passes");
        assertTrue(passes > 0, "Expects a count > 0");
    }

    @Test
    void assertThatTraceCountMetricIsReported() throws IOException {
        String resultJson = new String(
                Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));

        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['tracecount'].passes");
        assertTrue(passes > 0, "Expects a count > 0");
    }

    @Test
    void assertThatSampleCountMetricIsReported() throws IOException {
        String resultJson = new String(
                Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));

        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['samplecount'].passes");
        assertTrue(passes > 0, "Expects a count > 0");
    }

    @Test
    void assertThatResponseTimeMetricIsReported() throws IOException {
        String resultJson = new String(
                Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));

        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['response_time'].passes");
        assertTrue(passes > 0, "Expects a count > 0");
    }

    @Test
    void assertThatCodeStacktraceIsCaptured() throws IOException {
        String resultJson = new String(
                Files.readAllBytes(namingConventions.local.k6Results(Configs.E2E.config.agents().get(0))));

        double passes = ResultsCollector.read(resultJson, "$.root_group.checks.['code.stacktrace'].passes");
        assertTrue(passes > 0, "Expects a count > 0");
    }
}
