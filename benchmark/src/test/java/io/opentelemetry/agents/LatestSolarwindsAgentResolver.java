package io.opentelemetry.agents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

public class LatestSolarwindsAgentResolver {
    private static final String NH_URL = "https://agent-binaries.global.st-ssp.solarwinds.com/apm/java/latest/solarwinds-apm-agent.jar";
    private static final String AO_URL = "https://files.appoptics.com/java/latest/appoptics-agent.jar";
    private static final String NH_AGENT_JAR_NAME = "solarwinds-apm-agent.jar";
    private static final String AO_AGENT_JAR_NAME = "appoptics-agent.jar";
    public static boolean useAOAgent = "AO".equals(System.getenv("AGENT_TYPE"));

    Optional<Path> resolve() throws Exception {
        return Optional.of(downloadAgent());
    }

    private Path downloadAgent() throws Exception {
        String assetURL;
        if (!useAOAgent) {
            assetURL = NH_URL;
        } else {
            assetURL = AO_URL;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(assetURL)
                .header("Authorization", "token " + System.getenv("GP_TOKEN"))
                .header("Accept", "application/octet-stream").build();

        Path path = Paths.get(".", useAOAgent ? AO_AGENT_JAR_NAME : NH_AGENT_JAR_NAME);
        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            byte[] fileRaw = response.body().bytes();
            Files.write(
                    path,
                    fileRaw,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        }
        return path;
    }
}
