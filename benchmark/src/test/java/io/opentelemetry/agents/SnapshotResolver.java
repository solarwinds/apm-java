package io.opentelemetry.agents;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SnapshotResolver {
  private static final String SNAPSHOT_URL =
      "https://agent-binaries.global.st-ssp.solarwinds.com/apm/java/latest/solarwinds-apm-agent.jar";
  private static final String SWO_AGENT_JAR_NAME = "solarwinds-apm-agent.jar";

  Optional<Path> resolve() throws Exception {
    return Optional.of(downloadAgent());
  }

  private Path downloadAgent() throws Exception {
    OkHttpClient client = new OkHttpClient();
    Request request =
        new Request.Builder()
            .url(SNAPSHOT_URL)
            .build();

    Path path = Paths.get(".", SWO_AGENT_JAR_NAME);
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
