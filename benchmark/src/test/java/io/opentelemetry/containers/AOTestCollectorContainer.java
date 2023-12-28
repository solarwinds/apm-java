package io.opentelemetry.containers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class AOTestCollectorContainer {
    static final int COLLECTOR_PORT = 12223;
    static final int COLLECTOR_HEALTH_CHECK_PORT = 8181;

    private static final Logger logger = LoggerFactory.getLogger(CollectorContainer.class);

    static {
        // needs to be executed, before Docker images are resolved
        if (System.getenv("GITHUB_USERNAME") != null && System.getenv("GITHUB_TOKEN") != null) {
            System.setProperty("registry.username", System.getenv("GITHUB_USERNAME"));
            System.setProperty("registry.password", System.getenv("GITHUB_TOKEN"));
        } else {
            throw new RuntimeException("GP_USERNAME and GP_TOKEN are needed.");
        }
    }

    public static GenericContainer<?> build(Network network) {
        return new GenericContainer<>(
                DockerImageName.parse("ghcr.io/solarwinds-cloud/apm-agent-test-collector:v2.0.4"))
                .withNetwork(network)
                .withNetworkAliases("AOCollector")
                .withLogConsumer(new Slf4jLogConsumer(logger))
                .withExposedPorts(COLLECTOR_PORT, COLLECTOR_HEALTH_CHECK_PORT)
                .waitingFor(Wait.forHttp("/collectors").forPort(COLLECTOR_HEALTH_CHECK_PORT))
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("test-server-grpc.crt"), "/server-grpc.crt")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("test-server-grpc.pem"), "/server-grpc.pem");
    }
}
