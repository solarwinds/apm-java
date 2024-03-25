/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

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

public interface AgentResolver {
  Optional<Path> resolve(Agent agent);

  default Path downloadAgent(String url, String filename) {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(url)
        .header("Accept", "application/octet-stream").build();

    Path path = Paths.get(".", filename);
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
