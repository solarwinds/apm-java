package io.opentelemetry.agents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;

public class LatestNighthawkAgentResolver {
    private static String NH_URL = "https://api.github.com/repos/appoptics/opentelemetry-java-instrumentation-custom-distro/releases/latest";
    Optional<Path> resolve() throws Exception {
        return Optional.of(downloadAgent());
    }

    private Path downloadAgent() throws Exception {
        Request request = new Request.Builder().url(NH_URL)
                .header("Authorization", "token " + System.getenv("GP_TOKEN")) // TODO
                .header("Accept", "application/vnd.github.v3+json").build();
        OkHttpClient client = new OkHttpClient();
        Response response = client.newCall(request).execute();
        byte[] raw = response.body().bytes();

        System.err.println("==========================agent-meta==================\n\n" + new String(raw));
        ObjectMapper mapper = new ObjectMapper();
        GithubRelease release = mapper.readValue(raw, GithubRelease.class);
        String assetURL = null;
        for (Asset asset : release.assets) {
            if (asset.name.equals("solarwinds-apm-agent-all.jar")) {
                assetURL = asset.url;
                break;
            }
        }
        if (assetURL == null) {
            throw new RuntimeException("Asset url not found for the NH agent.");
        }

        request = new Request.Builder().url(assetURL)
                .header("Authorization", "token " + System.getenv("GP_TOKEN")) // TODO
                .header("Accept", "application/octet-stream").build();

        response = client.newCall(request).execute();
        byte[] fileRaw = response.body().bytes();
        Path path = Paths.get(".", "solarwinds-apm-agent-all.jar");
        Files.write(
                path,
                fileRaw,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
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