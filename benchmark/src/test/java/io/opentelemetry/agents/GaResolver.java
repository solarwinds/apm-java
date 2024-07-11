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

public class GaResolver {
  private static final String GA_URL = "https://api.github.com/repos/solarwinds/apm-java/releases";
  private static final String SWO_AGENT_JAR_NAME = "solarwinds-apm-agent.jar";

  Optional<Path> resolve() throws Exception {
    return Optional.of(downloadAgent());
  }

  private Path downloadAgent() throws Exception {
    String assetURL = null;
    OkHttpClient client = new OkHttpClient();
    Request request =
        new Request.Builder()
            .url(GA_URL)
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
          if (asset.name.equals(SWO_AGENT_JAR_NAME)) {
            assetURL = asset.url;
            break outerLoop;
          }
        }
      }

      if (assetURL == null) {
        throw new RuntimeException("Asset url not found for the NH agent.");
      }
    }

    Request downloadRequest =
        new Request.Builder()
            .url(assetURL)
            .header("Authorization", "token " + System.getenv("GITHUB_TOKEN"))
            .header("Accept", "application/octet-stream")
            .build();

    Path path = Paths.get(".", SWO_AGENT_JAR_NAME);
    try (Response response = client.newCall(downloadRequest).execute()) {
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
