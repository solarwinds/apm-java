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

public class SpringBootWebMvcContainer implements Container {

  private static final Logger logger = LoggerFactory.getLogger(SpringBootWebMvcContainer.class);
  private static final int SERVER_PORT = 8080;
  private final AgentResolver agentResolver;

  private final Network network;
  private final Agent agent;

  public SpringBootWebMvcContainer(AgentResolver agentResolver, Network network, Agent agent) {
    this.agentResolver = agentResolver;
    this.network = network;
    this.agent = agent;
  }

  public GenericContainer<?> build() {
    Path agentPath = agentResolver.resolve(this.agent).orElseThrow();
    return new GenericContainer<>(DockerImageName.parse("smt:webmvc"))
            .withNetwork(network)
            .withNetworkAliases("webmvc")
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withExposedPorts(SERVER_PORT)
            .waitingFor(Wait.forHttp("/actuator/health").withReadTimeout(Duration.ofMinutes(5)).forPort(SERVER_PORT))
            .withEnv("SERVER_PORT", String.format("%d", SERVER_PORT))
            .withEnv("SW_APM_CONFIG_FILE", "/app/apm-config.json")
            .withEnv("SW_APM_DEBUG_LEVEL", "trace")
            .withEnv("SW_APM_COLLECTOR", System.getenv("SW_APM_COLLECTOR"))
            .withEnv("SW_APM_SERVICE_KEY", System.getenv("SW_APM_SERVICE_KEY") + ":java-apm-smoke-test-webmvc")
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
    result.add("-Dotel.javaagent.extensions=/app/custom-extensions.jar");

    result.addAll(this.agent.getAdditionalJvmArgs());
    result.add("-javaagent:/app/" + agentJarPath.getFileName());
    result.add("-jar");

    result.add("/app/spring-boot-webmvc.jar");
    return result.toArray(new String[] {});
  }
}
