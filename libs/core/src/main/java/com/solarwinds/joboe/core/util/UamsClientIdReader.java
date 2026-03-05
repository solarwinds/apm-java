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

package com.solarwinds.joboe.core.util;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import io.opentelemetry.api.internal.InstrumentationUtil;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONException;
import org.json.JSONObject;

public class UamsClientIdReader {
  private static final Logger logger = LoggerFactory.getLogger();
  private static final HostInfoUtils.OsType osType = HostInfoUtils.getOsType();
  private static final Path uamsClientIdPath;
  private static final AtomicReference<String> uamsClientId = new AtomicReference<>();
  private static final AtomicReference<FileTime> lastModified =
      new AtomicReference<>(FileTime.from(Instant.EPOCH));

  static {
    if (osType == HostInfoUtils.OsType.WINDOWS) {
      String programData = System.getenv("PROGRAMDATA");
      if (programData == null) {
        programData = "C:\\ProgramData\\";
      }
      uamsClientIdPath = Paths.get(programData, "SolarWinds", "UAMSClient", "uamsclientid");
    } else {
      uamsClientIdPath = Paths.get("/", "opt", "solarwinds", "uamsclient", "var", "uamsclientid");
    }
    logger.debug("Set uamsclientid path to " + uamsClientIdPath);
  }

  public static String getUamsClientId() {
    try {
      FileTime modifiedTime = Files.getLastModifiedTime(uamsClientIdPath);
      if (!lastModified.get().equals(modifiedTime)) {
        lastModified.set(modifiedTime);
        uamsClientId.set(sanitize(readFirstLine(uamsClientIdPath)));
        logger.debug(
            "Updated uamsclientid to " + uamsClientId.get() + ", lastModifiedTime=" + modifiedTime);
      }
    } catch (IOException e) {
      logger.debug(String.format("Cannot read the file[%s] due error: %s", uamsClientIdPath, e));
      getUamsClientIdViaRestApi().ifPresent(uamsClientId::set);
    }
    return uamsClientId.get();
  }

  static Optional<String> getUamsClientIdViaRestApi() {
    AtomicReference<Optional<String>> result = new AtomicReference<>(Optional.empty());
    InstrumentationUtil.suppressInstrumentation(
        () -> {
          HttpURLConnection connection = null;
          try {
            connection =
                (HttpURLConnection)
                    new URL("http://127.0.0.1:2113/info/uamsclient").openConnection(Proxy.NO_PROXY);
            connection.setRequestMethod("GET");

            int statusCode = connection.getResponseCode();
            if (statusCode >= 200 && statusCode < 300) {
              String payload;
              try (BufferedReader reader =
                  new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                  sb.append(line);
                }
                payload = sb.toString();
              }
              JSONObject jsonPayload = new JSONObject(payload);
              String clientId = jsonPayload.getString("uamsclient_id");

              logger.debug(
                  String.format(
                      "Got UAMS client ID(%s) from API, using hardcoded endpoint", clientId));
              result.set(Optional.ofNullable(clientId));
            } else {
              logger.debug(
                  String.format(
                      "Request to UAMS REST endpoint failed. Status=%d, payload=%s",
                      statusCode, null));
            }

          } catch (IOException | JSONException exception) {
            logger.debug(String.format("Error reading from UAMS REST endpoint\n%s", exception));
          } finally {
            if (connection != null) {
              connection.disconnect();
            }
          }
        });
    return result.get();
  }

  private static String readFirstLine(Path filePath) throws IOException {
    String line = null;
    try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
      line = br.readLine();
    }
    return line;
  }

  private static String sanitize(String id) {
    String res = null;
    try {
      if (id.length() != 36) { // UUID in 8-4-4-4-12 format
        throw new IllegalArgumentException("incorrect length");
      }

      String[] parts = id.split("-");
      if (parts.length != 5) {
        throw new IllegalArgumentException("incorrect format");
      }
      res = id;
    } catch (IllegalArgumentException e) {
      logger.debug("Discarded invalid UAMS client id: " + id, e);
    }
    return res;
  }
}
