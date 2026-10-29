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

import static com.solarwinds.opentelemetry.extensions.SharedNames.SW_OTEL_PROXY_HOST_KEY;
import static com.solarwinds.opentelemetry.extensions.SharedNames.SW_OTEL_PROXY_PORT_KEY;

import com.solarwinds.joboe.config.ProxyConfig;
import com.solarwinds.joboe.logging.LoggerFactory;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

public final class ProxyHelper {
  private ProxyHelper() {}

  public static boolean isProxyConfigured(ConfigProperties properties) {
    return properties.getString(SW_OTEL_PROXY_HOST_KEY) != null
        && properties.getInt(SW_OTEL_PROXY_PORT_KEY) != null;
  }

  public static ProxyConfig toProxyConfig(DeclarativeConfigProperties properties) {

    DeclarativeConfigProperties proxy = properties.getStructured("proxy");
    if (proxy == null) {
      return null;
    }

    String host = proxy.getString("host");
    Integer port = proxy.getInt("port");
    if (host == null || port == null || port < 1 || port > 65535) {
      LoggerFactory.getLogger()
          .warn(
              "Proxy is specified but no host or port specified or port is specified 1-65535 range");
      return null;
    }

    return new ProxyConfig(host, port, null, null);
  }
}
