/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
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
        .withCommand(
            "run",
            "-u",
            String.valueOf(config.concurrentConnections()),
            "-i",
            String.valueOf(config.totalIterations()),
            "--rps",
            String.valueOf(config.maxRequestRate()),
            "--summary-export",
            k6OutputFile.toString(),
            "/app/basic.js")
        .withStartupCheckStrategy(
            new OneShotStartupCheckStrategy().withTimeout(Duration.ofMinutes(150)));
  }
}
