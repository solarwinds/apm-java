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
