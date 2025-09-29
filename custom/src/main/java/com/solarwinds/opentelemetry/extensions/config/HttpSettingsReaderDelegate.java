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

package com.solarwinds.opentelemetry.extensions.config;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.ProxyConfig;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.joboe.shaded.google.gson.Gson;
import com.solarwinds.joboe.shaded.google.gson.GsonBuilder;
import io.opentelemetry.api.internal.InstrumentationUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

public class HttpSettingsReaderDelegate {

  private static final Logger logger = LoggerFactory.getLogger();

  private static final Gson gson = new GsonBuilder().create();

  public Settings fetchSettings(String url, String authorizationHeader) {
    AtomicReference<Settings> settings = new AtomicReference<>();
    InstrumentationUtil.suppressInstrumentation(
        () -> {
          HttpURLConnection connection = null;
          try {
            connection = getHttpUrlConnection(new URL(url), authorizationHeader);
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
              String errorResponse = getErrorMessage(connection);
              logger.error(
                  String.format(
                      "HTTP request failed with status code: %s and error: %s",
                      responseCode, errorResponse));
            } else {
              try (BufferedReader reader =
                  new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                settings.set(JsonSettingWrapper.wrap(gson.fromJson(reader, JsonSetting.class)));
              }
            }

          } catch (IOException e) {
            logger.error(String.format("IO error while fetching settings from URL: %s", url), e);

          } catch (Exception e) {
            logger.error(String.format("Failed to get settings from http URL: %s", url), e);
          } finally {
            if (connection != null) {
              connection.disconnect();
            }
          }
        });

    return settings.get();
  }

  private String getErrorMessage(HttpURLConnection connection) {
    String errorResponse;
    try (BufferedReader errorReader =
        new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
      StringBuilder errorBuilder = new StringBuilder();
      String line;
      while ((line = errorReader.readLine()) != null) {
        errorBuilder.append(line);
      }
      errorResponse = errorBuilder.toString();
    } catch (Exception e) {
      errorResponse = "Unable to read error response";
    }
    return errorResponse;
  }

  HttpURLConnection getHttpUrlConnection(URL url, String authorizationHeader) throws IOException {
    HttpURLConnection connection;
    ProxyConfig proxyConfig = (ProxyConfig) ConfigManager.getConfig(ConfigProperty.AGENT_PROXY);
    if (proxyConfig != null) {
      Proxy proxy =
          new Proxy(
              Proxy.Type.HTTP, new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort()));
      connection = (HttpURLConnection) url.openConnection(proxy);
    } else {
      connection = (HttpURLConnection) url.openConnection();
    }

    connection.setRequestMethod("GET");
    connection.setRequestProperty("Authorization", authorizationHeader);
    connection.setRequestProperty("Content-Type", "application/json");

    connection.setRequestProperty("Accept", "application/json");
    connection.setConnectTimeout(10000);
    connection.setReadTimeout(10000);

    return connection;
  }
}
