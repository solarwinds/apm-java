/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.containers;

import io.opentelemetry.agents.Agent;
import io.opentelemetry.agents.AgentResolver;
import io.opentelemetry.agents.LatestSolarwindsAgentResolver;
import io.opentelemetry.util.NamingConventions;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class PetClinicRestContainer {

  private static final Logger logger = LoggerFactory.getLogger(PetClinicRestContainer.class);
  private static final int PETCLINIC_PORT = 9966;
  private final AgentResolver agentResolver = new AgentResolver();

  private final Network network;
  private final Startable collector;
  private final Agent agent;
  private final NamingConventions namingConventions;

  public PetClinicRestContainer(
      Network network, Startable collector, Agent agent, NamingConventions namingConventions) {
    this.network = network;
    this.collector = collector;
    this.agent = agent;
    this.namingConventions = namingConventions;
  }

  public GenericContainer<?> build() throws Exception {

    Optional<Path> agentJar = agentResolver.resolve(this.agent);

    GenericContainer<?> container =
        new GenericContainer<>(
                DockerImageName.parse(
                    "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/petclinic-rest-base:latest"))
            .withNetwork(network)
            .withNetworkAliases("petclinic")
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withExposedPorts(PETCLINIC_PORT)
            .withFileSystemBind(
                namingConventions.localResults(), namingConventions.containerResults())
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("overhead.jfc"), "/app/overhead.jfc")
            .waitingFor(Wait.forHttp("/petclinic/actuator/health").forPort(PETCLINIC_PORT))
            .withEnv("spring_profiles_active", "postgresql,spring-data-jpa")
            .withEnv(
                "spring_datasource_url",
                "jdbc:postgresql://postgres:5432/" + PostgresContainer.DATABASE_NAME)
            .withEnv("spring_datasource_username", PostgresContainer.USERNAME)
            .withEnv("spring_datasource_password", PostgresContainer.PASSWORD)
            .withEnv("spring_jpa_hibernate_ddl-auto", "none")
            .withEnv("SW_APM_DEBUG_LEVEL", "info")
            .withEnv("SW_APM_COLLECTOR", "AOCollector:12223")
            .withEnv("SW_APM_TRUSTEDPATH", "/test-server-grpc.crt")
                .withEnv("APPOPTICS_DEBUG_LEVEL", "info")
                .withEnv("APPOPTICS_COLLECTOR", "AOCollector:12223")
                .withEnv("APPOPTICS_TRUSTEDPATH", "/test-server-grpc.crt")
            .withCopyFileToContainer(
                        MountableFile.forClasspathResource("test-server-grpc.crt"), "/test-server-grpc.crt")
            .dependsOn(collector)
            .withCommand(buildCommandline(agentJar));
    agentJar.ifPresent(
        agentPath ->
            container.withCopyFileToContainer(
                MountableFile.forHostPath(agentPath),
                "/app/" + agentPath.getFileName().toString()));
    return container;
  }

  @NotNull
  private String[] buildCommandline(Optional<Path> agentJar) {
    List<String> result = new ArrayList<>();
    if (!agent.equals(Agent.NH_LATEST_RELEASE)) {
      result.addAll(
              Arrays.asList(
                      "java",
                      "-Dotel.traces.exporter=otlp",
                      "-Dotel.imr.export.interval=5000",
                      "-Dotel.exporter.otlp.insecure=true",
                      "-Dotel.exporter.otlp.protocol=grpc",
                      "-Dotel.exporter.otlp.endpoint=http://collector:4317",
                      "-Dotel.resource.attributes=service.name=petclinic-otel-overhead"));
    } else {
      result.addAll(Arrays.asList("java",
              "-Dotel.solarwinds.service.key=" + System.getenv("SW_APM_SERVICE_KEY") + ":sw-java-benchmark"));
    }
    result.addAll(this.agent.getAdditionalJvmArgs());
    agentJar.ifPresent(path -> result.add("-javaagent:/app/" + path.getFileName() + (LatestSolarwindsAgentResolver.useAOAgent ? "=service_key="+System.getenv("SW_APM_SERVICE_KEY")+":sw-java-benchmark":"")));
    result.add("-jar");
    result.add("/app/spring-petclinic-rest.jar");
    System.err.println("Running app with command:\n" + String.join(" ", result));
    return result.toArray(new String[] {});
  }
}
