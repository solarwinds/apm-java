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

package com.solarwinds.opentelemetry.extensions.config.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.config.ProxyConfig;
import com.solarwinds.joboe.sampling.SamplingConfiguration;
import com.solarwinds.joboe.sampling.SettingsManager;
import com.solarwinds.opentelemetry.extensions.SolarwindsSampler;
import com.solarwinds.opentelemetry.extensions.config.HttpSettingsReaderDelegate;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class HttpSamplerComponentProviderTest {
  private static final String SERVICE_KEY_ONLY =
      "swo_http:\n  service_key: sampler-key:sampler-service\n";

  private final HttpSettingsReaderDelegate delegate = mock(HttpSettingsReaderDelegate.class);

  private final AtomicReference<ProxyConfig> proxyConfig = new AtomicReference<>();

  private final HttpSamplerComponentProvider tested =
      new HttpSamplerComponentProvider(
          proxy -> {
            proxyConfig.set(proxy);
            return delegate;
          });

  @BeforeEach
  void setUp() {
    ConfigManager.reset();
    SettingsManager.reset();
  }

  @AfterEach
  void tearDown() {
    ConfigManager.reset();
    SettingsManager.reset();
  }

  @Test
  void testName() {
    assertEquals("swo_http", tested.getName());
  }

  @Test
  void testCreateFetchesSettingsWithSamplerConfig() throws InterruptedException {
    assertInstanceOf(
        SolarwindsSampler.class,
        tested.create(
            fromYaml(
                "swo_http:\n"
                    + "  collector: apm.collector.na-01.st-ssp.solarwinds.com\n"
                    + "  service_key: sampler-key:sampler-service\n"
                    + "  proxy:\n"
                    + "    host: proxy.example.com\n"
                    + "    port: 8080\n")));

    awaitFirstPoll();
    verify(delegate)
        .fetchSettings(
            "https://apm.collector.na-01.st-ssp.solarwinds.com/v1/settings/sampler-service",
            "Bearer sampler-key");

    assertEquals("proxy.example.com", proxyConfig.get().getHost());
    assertEquals(8080, proxyConfig.get().getPort());
    assertNull(ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY));

    assertNull(ConfigManager.getConfig(ConfigProperty.AGENT_COLLECTOR));
    assertNull(ConfigManager.getConfig(ConfigProperty.AGENT_PROXY));
  }

  @Test
  void testCreateIgnoresSolarwindsConfig() throws InterruptedException, InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_COLLECTOR, "global.collector.com");
    ConfigManager.setConfig(ConfigProperty.AGENT_SERVICE_KEY, "global-key:global-service");
    ConfigManager.setConfig(ConfigProperty.AGENT_CONTEXT_TTL, 1000);

    tested.create(fromYaml(SERVICE_KEY_ONLY));
    awaitFirstPoll();

    verify(delegate)
        .fetchSettings(
            "https://apm.collector.na-01.cloud.solarwinds.com/v1/settings/sampler-service",
            "Bearer sampler-key");
    assertNull(proxyConfig.get());
    assertEquals(1_200_000, SettingsManager.getSamplingConfiguration().getTtl());
  }

  @Test
  void testCreateTwiceClosesPreviousFetcher() {
    tested.create(fromYaml(SERVICE_KEY_ONLY));
    CountDownLatch first = SettingsManager.getSettingsAvailableLatch();

    tested.create(fromYaml(SERVICE_KEY_ONLY));
    assertNotSame(first, SettingsManager.getSettingsAvailableLatch());
  }

  @Test
  void testCreateAppliesContextConfig() {
    tested.create(
        fromYaml(
            SERVICE_KEY_ONLY
                + "  context_ttl: 600000\n"
                + "  context_max_events: 5000\n"
                + "  context_max_backtraces: 50\n"));

    SamplingConfiguration samplingConfiguration = SettingsManager.getSamplingConfiguration();
    assertEquals(600000, samplingConfiguration.getTtl());
    assertEquals(5000, samplingConfiguration.getMaxEvents());

    assertEquals(50, samplingConfiguration.getMaxBacktraces());
    assertNull(ConfigManager.getConfig(ConfigProperty.AGENT_CONTEXT_TTL));
  }

  @ParameterizedTest
  @MethodSource("invalidConfigs")
  void testCreateThrowsForInvalidConfig(String yaml) {
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> tested.create(fromYaml(yaml)));
    assertInstanceOf(InvalidConfigException.class, exception.getCause());
  }

  static Stream<String> invalidConfigs() {
    return Stream.of(
        "swo_http:\n  collector: apm.collector.na-01.cloud.solarwinds.com\n",
        "swo_http:\n  service_key: ''\n",
        SERVICE_KEY_ONLY + "  collector: ' '\n",
        SERVICE_KEY_ONLY + "  context_ttl: 0\n",
        SERVICE_KEY_ONLY + "  context_max_events: -1\n",
        SERVICE_KEY_ONLY + "  context_max_backtraces: 0\n",
        SERVICE_KEY_ONLY + "  proxy:\n    host: proxy.example.com\n",
        SERVICE_KEY_ONLY + "  proxy:\n    port: 8080\n",
        SERVICE_KEY_ONLY + "  proxy:\n    host: proxy.example.com\n    port: 70000\n");
  }

  @Test
  void testCreateThrowsWhenServiceKeyMissing() {
    RuntimeException exception =
        assertThrows(
            RuntimeException.class, () -> tested.create(DeclarativeConfigProperties.empty()));
    assertInstanceOf(InvalidConfigException.class, exception.getCause());
  }

  private void awaitFirstPoll() throws InterruptedException {
    assertTrue(SettingsManager.getSettingsAvailableLatch().await(5, TimeUnit.SECONDS));
    verify(delegate).fetchSettings(anyString(), anyString());
  }

  private DeclarativeConfigProperties fromYaml(String yaml) {
    return DeclarativeConfiguration.toConfigProperties(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
        .getStructured("swo_http");
  }
}
