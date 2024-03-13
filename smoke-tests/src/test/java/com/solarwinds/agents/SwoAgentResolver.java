package com.solarwinds.agents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SwoAgentResolver implements AgentResolver {
  private static final String NH_URL = "https://agent-binaries.global.st-ssp.solarwinds.com/apm/java/latest/solarwinds-apm-agent.jar";

  private static final String NH_AGENT_JAR_NAME = "solarwinds-apm-agent.jar";


  public Optional<Path> resolve(Agent agent) {
    return Optional.ofNullable(downloadAgent());
  }

  private Path downloadAgent() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(NH_URL)
                .header("Accept", "application/octet-stream").build();

        Path path = Paths.get(".", NH_AGENT_JAR_NAME);
        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            byte[] fileRaw = response.body().bytes();

            Files.write(
                    path,
                    fileRaw,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            return null;
        }
        return path;
  }
}
