package io.opentelemetry.agents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public class LatestSolarwindsAgentResolver {
  private static final String NH_URL = "https://api.github.com/repos/solarwinds/apm-java/releases";
  private static final String AO_URL =
      "https://files.appoptics.com/java/latest/appoptics-agent.jar";
  private static final String NH_AGENT_JAR_NAME = "solarwinds-apm-agent.jar";
  private static final String AO_AGENT_JAR_NAME = "appoptics-agent.jar";
  public static boolean useAOAgent = "AO".equals(System.getenv("AGENT_TYPE"));

  Optional<Path> resolve() throws Exception {
    return Optional.of(downloadAgent());
  }

  private Path downloadAgent() throws Exception {
    String assetURL;
    OkHttpClient client = new OkHttpClient();
    if (useAOAgent) {
      assetURL = AO_URL;
    } else {
      assetURL = NH_URL;
      Request request =
          new Request.Builder()
              .url(NH_URL)
              .header("Authorization", "token " + System.getenv("GITHUB_TOKEN"))
              .header("Accept", "application/vnd.github.v3+json")
              .build();

      try (Response response = client.newCall(request).execute()) {
        assert response.body() != null;
        byte[] raw = response.body().bytes();

        ObjectMapper mapper = new ObjectMapper();
        List<GithubRelease> releases =
            mapper.readValue(raw, new TypeReference<List<GithubRelease>>() {});

        outerLoop:
        for (GithubRelease release : releases) {
          for (Asset asset : release.assets) {
            if (asset.name.equals(NH_AGENT_JAR_NAME)) {
              assetURL = asset.url;
              break outerLoop;
            }
          }
        }
        if (assetURL == null) {
          throw new RuntimeException("Asset url not found for the NH agent.");
        }
      }
    }

    Request request =
        new Request.Builder()
            .url(assetURL)
            .header("Authorization", "token " + System.getenv("GITHUB_TOKEN"))
            .header("Accept", "application/octet-stream")
            .build();

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

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class GithubRelease {
    private List<Asset> assets;

    public List<Asset> getAssets() {
      return assets;
    }

    public void setAssets(List<Asset> assets) {
      this.assets = assets;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Asset {
    private String url;
    private String name;

    public String getUrl() {
      return url;
    }

    public String getName() {
      return name;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public void setName(String name) {
      this.name = name;
    }
  }
}
