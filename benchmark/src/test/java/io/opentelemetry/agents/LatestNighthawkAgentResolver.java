package io.opentelemetry.agents;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.Optional;

public class LatestNighthawkAgentResolver {
    Optional<Path> resolve(URL url) throws Exception {
        if (url != null) {
            return Optional.of(downloadAgent(url));
        }
        throw new IllegalArgumentException("Unknown agent: " + url);
    }

    private Path downloadAgent(URL agentUrl) throws Exception {
        if (agentUrl.getProtocol().equals("file")) {
            Path source = Path.of(agentUrl.toURI());
            Path result = Paths.get(".", source.getFileName().toString());
            Files.copy(source, result, StandardCopyOption.REPLACE_EXISTING);
            return result;
        }
        Request request = new Request.Builder().url(agentUrl).build();
        OkHttpClient client = new OkHttpClient();
        Response response = client.newCall(request).execute();
        byte[] raw = response.body().bytes();
        Path path = Paths.get(".", "solarwinds-apm-agent-all.jar");
        Files.write(
                path,
                raw,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
        return path;
    }
}
