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

package com.solarwinds.containers;

import com.solarwinds.agents.Agent;
import com.solarwinds.agents.AgentResolver;
import com.solarwinds.util.NamingConventions;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class PetClinicRestContainer implements Container {

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

  public GenericContainer<?> build() {
    Path agentPath = agentResolver.resolve(this.agent).orElseThrow();
    if (Objects.equals(System.getenv("LAMBDA"), "true")) {
      return new GenericContainer<>(
          DockerImageName.parse(
              "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/petclinic-rest-base:20230601125442"))
          .withNetwork(network)
          .withNetworkAliases("petclinic")
          .withLogConsumer(new Slf4jLogConsumer(logger))
          .withExposedPorts(PETCLINIC_PORT)
          .withFileSystemBind(namingConventions.localResults(),
              namingConventions.containerResults())
          .withFileSystemBind("./solarwinds-apm-settings.json", "/tmp/solarwinds-apm-settings.json")
          .withEnv("OTEL_JAVAAGENT_DEBUG", "true")
          .withEnv("SW_APM_SQL_TAG", "true")
          .withEnv("SW_APM_SQL_TAG_PREPARED", "true")
          .withEnv("SW_APM_SQL_TAG_DATABASES", "postgresql")
          .waitingFor(
              Wait.forHttp("/petclinic/actuator/health").withReadTimeout(Duration.ofMinutes(5))
                  .forPort(PETCLINIC_PORT))
          .withEnv("spring_profiles_active", "postgresql,spring-data-jpa")
          .withEnv(
              "spring_datasource_url",
              "jdbc:postgresql://postgres:5432/" + PostgresContainer.DATABASE_NAME)
          .withEnv("spring_datasource_username", PostgresContainer.USERNAME)
          .withEnv("spring_datasource_password", PostgresContainer.PASSWORD)
          .withEnv("spring_jpa_hibernate_ddl-auto", "none")
          .withEnv("SW_APM_DEBUG_LEVEL", "trace")
          .withEnv("OTEL_EXPORTER_OTLP_ENDPOINT", System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT"))
          .withEnv("OTEL_EXPORTER_OTLP_HEADERS",
              String.format("authorization=Bearer %s", System.getenv("SW_APM_SERVICE_KEY")))
          .withEnv("OTEL_SERVICE_NAME", "lambda-e2e")
          .withEnv("SW_APM_TRANSACTION_NAME", "lambda-test-txn")
          .withEnv("SW_APM_EXPORT_LOGS_ENABLED", "true")
          .withStartupTimeout(Duration.ofMinutes(5))
          .withCopyFileToContainer(
              MountableFile.forHostPath(agentPath),
              "/app/" + agentPath.getFileName())
          .withCommand(buildCommandline(agentPath));
    }

    return new GenericContainer<>(
            DockerImageName.parse(
                    "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/petclinic-rest-base:20230601125442"))
            .withNetwork(network)
            .withNetworkAliases("petclinic")
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withExposedPorts(PETCLINIC_PORT)
            .withFileSystemBind(namingConventions.localResults(), namingConventions.containerResults())
            .withFileSystemBind("./apm-config.json", "/app/apm-config.json")
            .withEnv("SW_APM_CONFIG_FILE", "/app/apm-config.json")
            .withEnv("OTEL_JAVAAGENT_DEBUG", "true")
            .waitingFor(Wait.forHttp("/petclinic/actuator/health").withReadTimeout(Duration.ofMinutes(5)).forPort(PETCLINIC_PORT))
            .withEnv("spring_profiles_active", "postgresql,spring-data-jpa")
            .withEnv(
                    "spring_datasource_url",
                    "jdbc:postgresql://postgres:5432/" + PostgresContainer.DATABASE_NAME)
            .withEnv("spring_datasource_username", PostgresContainer.USERNAME)
            .withEnv("spring_datasource_password", PostgresContainer.PASSWORD)
            .withEnv("spring_jpa_hibernate_ddl-auto", "none")
            .withEnv("SW_APM_DEBUG_LEVEL", "trace")
            .withEnv("SW_APM_SQL_TAG", "true")
            .withEnv("SW_APM_SQL_TAG_DATABASES", "postgresql")
            .withEnv("SW_APM_SQL_TAG_PREPARED", "true")
            .withEnv("SW_APM_EXPORT_LOGS_ENABLED", "true")
            .withEnv("SW_APM_COLLECTOR", System.getenv("SW_APM_COLLECTOR"))
            .withEnv("SW_APM_SERVICE_KEY", String.format("%s:java-apm-smoke-test", System.getenv("SW_APM_SERVICE_KEY")))
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
