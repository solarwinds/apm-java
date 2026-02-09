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

package com.solarwinds.opentelemetry.extensions.config.parser.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.config.ProxyConfig;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import org.junit.jupiter.api.Test;

class ProxyParserTest {

  private final ProxyParser tested = new ProxyParser();

  @Test
  void testConfigKey() {
    assertEquals("agent.proxy", tested.configKey());
  }

  @Test
  void testConvertReturnsNullWhenProxyStringIsNull() throws InvalidConfigException {
    DeclarativeConfigProperties configProperties = mock(DeclarativeConfigProperties.class);
    when(configProperties.getString("agent.proxy")).thenReturn(null);

    ProxyConfig result = tested.convert(configProperties);

    assertNull(result);
  }

  @Test
  void testConvertReturnsProxyConfigWithHostAndPort() throws InvalidConfigException {
    DeclarativeConfigProperties configProperties = mock(DeclarativeConfigProperties.class);
    when(configProperties.getString("agent.proxy")).thenReturn("http://proxy.example.com:8080");

    ProxyConfig result = tested.convert(configProperties);

    assertEquals("proxy.example.com", result.getHost());
    assertEquals(8080, result.getPort());
    assertNull(result.getUsername());
    assertNull(result.getPassword());
  }

  @Test
  void testConvertReturnsProxyConfigWithCredentials() throws InvalidConfigException {
    DeclarativeConfigProperties configProperties = mock(DeclarativeConfigProperties.class);
    when(configProperties.getString("agent.proxy"))
        .thenReturn("http://user:password@proxy.example.com:3128");

    ProxyConfig result = tested.convert(configProperties);

    assertEquals("proxy.example.com", result.getHost());
    assertEquals(3128, result.getPort());
    assertEquals("user", result.getUsername());
    assertEquals("password", result.getPassword());
  }
}
