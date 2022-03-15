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
        System.setProperty("registry.username", System.getenv("GP_USERNAME"));
        System.setProperty("registry.password", System.getenv("GP_TOKEN"));
    }

    public static GenericContainer<?> build(Network network) {

        return new GenericContainer<>(
                DockerImageName.parse("ghcr.io/librato/apm-agent-test-collector:v1.1.0"))
                .withNetwork(network)
                .withNetworkAliases("AOCollector")
                .withLogConsumer(new Slf4jLogConsumer(logger))
                .withExposedPorts(COLLECTOR_PORT, COLLECTOR_HEALTH_CHECK_PORT)
                .waitingFor(Wait.forHttp("/collectors").forPort(COLLECTOR_HEALTH_CHECK_PORT))
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("test-server-grpc.crt"), "/server-grpc.crt")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("test-server-grpc.pem"), "/server-grpc.pem");
//                .withCommand("--config /etc/otel.yaml");
    }
}
