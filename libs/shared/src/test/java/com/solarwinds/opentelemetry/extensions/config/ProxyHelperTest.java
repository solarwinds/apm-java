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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.config.ProxyConfig;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProxyHelperTest {

  @Mock private ConfigProperties configProperties;

  @Test
  void shouldReturnTrueWhenProxyConfigured() {
    when(configProperties.getString(eq(SW_OTEL_PROXY_HOST_KEY))).thenReturn("localhost");
    when(configProperties.getInt(eq(SW_OTEL_PROXY_PORT_KEY))).thenReturn(8080);

    assertTrue(ProxyHelper.isProxyConfigured(configProperties));
  }

  @Test
  void shouldReturnFalseWhenProxyHostMissing() {
    when(configProperties.getString(eq(SW_OTEL_PROXY_HOST_KEY))).thenReturn(null);
    assertFalse(ProxyHelper.isProxyConfigured(configProperties));
  }

  @Test
  void shouldReturnFalseWhenProxyPortMissing() {
    when(configProperties.getString(eq(SW_OTEL_PROXY_HOST_KEY))).thenReturn("localhost");
    when(configProperties.getInt(eq(SW_OTEL_PROXY_PORT_KEY))).thenReturn(null);

    assertFalse(ProxyHelper.isProxyConfigured(configProperties));
  }

  @Test
  void shouldReturnProxyConfigWhenHostAndPortPresent() {
    ProxyConfig proxyConfig =
        ProxyHelper.toProxyConfig(
            fromYaml("swo_otlp:\n  proxy:\n    host: proxy.local\n    port: 3128\n"));

    assertEquals("proxy.local", proxyConfig.getHost());
    assertEquals(3128, proxyConfig.getPort());

    assertNull(proxyConfig.getUsername());
    assertNull(proxyConfig.getPassword());
  }

  @Test
  void shouldReturnNullWhenProxyAbsent() {
    assertNull(ProxyHelper.toProxyConfig(fromYaml("swo_otlp:\n  endpoint: https://localhost\n")));
  }

  @Test
  void shouldReturnNullWhenProxyHostAbsent() {
    assertNull(ProxyHelper.toProxyConfig(fromYaml("swo_otlp:\n  proxy:\n    port: 3128\n")));
  }

  @Test
  void shouldReturnNullWhenProxyPortAbsent() {
    assertNull(ProxyHelper.toProxyConfig(fromYaml("swo_otlp:\n  proxy:\n    host: proxy.local\n")));
  }

  private DeclarativeConfigProperties fromYaml(String yaml) {
    return DeclarativeConfiguration.toConfigProperties(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
        .getStructured("swo_otlp");
  }
}
