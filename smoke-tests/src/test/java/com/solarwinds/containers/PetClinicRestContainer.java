/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.solarwinds.containers;

import com.solarwinds.agents.Agent;
import com.solarwinds.agents.AgentResolver;
import com.solarwinds.util.NamingConventions;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class PetClinicRestContainer {

  private static final Logger logger = LoggerFactory.getLogger(PetClinicRestContainer.class);
  private static final int PETCLINIC_PORT = 9966;
  private final AgentResolver agentResolver;

  private final Network network;
  private final Agent agent;
  private final NamingConventions namingConventions;

  public PetClinicRestContainer(AgentResolver agentResolver, Network network, Agent agent, NamingConventions namingConventions) {
    this.agentResolver = agentResolver;
    this.network = network;
    this.agent = agent;
    this.namingConventions = namingConventions;
  }

  public GenericContainer<?> build() throws Exception {
    Path agentPath = agentResolver.resolve(this.agent).orElseThrow();
    return new GenericContainer<>(
            DockerImageName.parse(
                    "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/petclinic-rest-base:20230601125442"))
            .withNetwork(network)
            .withNetworkAliases("petclinic")
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withExposedPorts(PETCLINIC_PORT)
            .withFileSystemBind(namingConventions.localResults(), namingConventions.containerResults())
            .waitingFor(Wait.forHttp("/petclinic/actuator/health").withReadTimeout(Duration.ofMinutes(5)).forPort(PETCLINIC_PORT))
            .withEnv("spring_profiles_active", "postgresql,spring-data-jpa")
            .withEnv(
                    "spring_datasource_url",
                    "jdbc:postgresql://postgres:5432/" + PostgresContainer.DATABASE_NAME)
            .withEnv("spring_datasource_username", PostgresContainer.USERNAME)
            .withEnv("spring_datasource_password", PostgresContainer.PASSWORD)
            .withEnv("spring_jpa_hibernate_ddl-auto", "none")
            .withEnv("SW_APM_DEBUG_LEVEL", "trace")
            .withEnv("SW_APM_COLLECTOR", System.getenv("SW_APM_COLLECTOR"))
            .withEnv("SW_APM_SERVICE_KEY", System.getenv("SW_APM_SERVICE_KEY") + ":java-apm-smoke-test")
            .withEnv("OTEL_SERVICE_NAME", "java-apm-smoke-test")
            .withStartupTimeout(Duration.ofMinutes(5))
            .withCopyFileToContainer(
                    MountableFile.forHostPath(agentPath),
                    "/app/" + agentPath.getFileName())
            .withCommand(buildCommandline(agentPath));
  }

  @NotNull
  private String[] buildCommandline(Path agentJarPath) {
    List<String> result = new ArrayList<>();
    result.add("java");

    result.addAll(this.agent.getAdditionalJvmArgs());
    result.add("-javaagent:/app/" + agentJarPath.getFileName());
    result.add("-jar");

    result.add("/app/spring-petclinic-rest.jar");
    return result.toArray(new String[] {});
  }
}
