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
import com.solarwinds.config.TestConfig;
import com.solarwinds.util.NamingConventions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.time.Duration;

public class K6Container {
  private static final Logger logger = LoggerFactory.getLogger(K6Container.class);
  private final Network network;
  private final Agent agent;
  private final TestConfig config;
  private final NamingConventions namingConventions;

  public K6Container(
      Network network, Agent agent, TestConfig config, NamingConventions namingConvention) {
    this.network = network;
    this.agent = agent;
    this.config = config;
    this.namingConventions = namingConvention;
  }

  public GenericContainer<?> build() {
    Path k6OutputFile = namingConventions.container.k6Results(agent);
    return new GenericContainer<>(DockerImageName.parse("grafana/k6"))
        .withNetwork(network)
        .withNetworkAliases("k6")
        .withLogConsumer(new Slf4jLogConsumer(logger))
        .withCopyFileToContainer(MountableFile.forHostPath("./k6"), "/app")
        .withFileSystemBind(namingConventions.localResults(), namingConventions.containerResults())
        .withCreateContainerCmdModifier(cmd -> cmd.withUser("root"))
            .withEnv("SWO_HOST_URL", System.getenv("SWO_HOST_URL"))
            .withEnv("SWO_COOKIE", System.getenv("SWO_COOKIE"))
            .withEnv("SWO_XSR_TOKEN", System.getenv("SWO_XSR_TOKEN"))
            .withEnv("LAMBDA", System.getenv("LAMBDA"))
        .withCommand(
            "run",
            "--summary-export",
            k6OutputFile.toString(),
            "/app/basic.js")
        .withStartupCheckStrategy(
            new OneShotStartupCheckStrategy().withTimeout(Duration.ofMinutes(150)));
  }
}
