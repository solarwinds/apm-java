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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public class SquidContainer implements Container {
    private static final Logger logger = LoggerFactory.getLogger(SquidContainer.class);
    private static final int PROXY_PORT = 3128;
    private final Network network;

    public SquidContainer(Network network) {
        this.network = network;
    }

    @Override
    public GenericContainer<?> build() {
        return new GenericContainer<>(DockerImageName.parse("ubuntu/squid:5.2-22.04_beta"))
                .withNetwork(network)
                .withNetworkAliases("squid-proxy")
                .withLogConsumer(new Slf4jLogConsumer(logger))
                .withExposedPorts(PROXY_PORT)
                .withFileSystemBind("./squid/passwd", "/etc/squid/passwd")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
                .withStartupTimeout(Duration.ofMinutes(2));
    }
}
