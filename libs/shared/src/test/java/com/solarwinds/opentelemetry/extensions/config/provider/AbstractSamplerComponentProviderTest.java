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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.sampling.SamplingConfiguration;
import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.joboe.sampling.SettingsFetcher;
import com.solarwinds.joboe.sampling.SettingsManager;
import com.solarwinds.joboe.sampling.TraceConfig;
import com.solarwinds.joboe.sampling.TraceConfigs;
import com.solarwinds.joboe.sampling.TracingMode;
import com.solarwinds.opentelemetry.extensions.SolarwindsSampler;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AbstractSamplerComponentProviderTest {
  private final SettingsFetcher settingsFetcher = mock(SettingsFetcher.class);

  private final AbstractSamplerComponentProvider tested =
      new AbstractSamplerComponentProvider() {
        @Override
        public String getName() {
          return "test";
        }

        @Override
        protected SettingsFetcher createSettingsFetcher(DeclarativeConfigProperties properties) {
          return settingsFetcher;
        }
      };

  @AfterEach
  void tearDown() {
    ConfigManager.reset();
    SettingsManager.reset();
  }

  @Test
  void testCreateDoesNotWriteToConfigManager() {
    tested.create(loadConfig());

    assertNull(ConfigManager.getConfig(ConfigProperty.AGENT_TRACING_MODE));
    assertNull(ConfigManager.getConfig(ConfigProperty.AGENT_TRIGGER_TRACE_ENABLED));
    assertNull(ConfigManager.getConfig(ConfigProperty.AGENT_INTERNAL_TRANSACTION_SETTINGS));
  }

  @Test
  void testCreateInitializesSettingsManagerWithFetcher() {
    Settings settings = mock(Settings.class);
    when(settingsFetcher.getSettings()).thenReturn(settings);

    tested.create(DeclarativeConfigProperties.empty());
    assertSame(settings, SettingsManager.getSettings());
  }

  @Test
  void testCreateAppliesSamplerConfig() {
    Sampler sampler = tested.create(loadConfig());

    assertInstanceOf(SolarwindsSampler.class, sampler);
    assertEquals(TracingMode.ENABLED, samplingConfiguration().getTracingMode());
    assertTrue(samplingConfiguration().isTriggerTraceEnabled());
  }

  @Test
  void testTransactionSettingsTakePrecedenceOverUrlSampleRates() {
    tested.create(loadConfig());
    TraceConfigs traceConfigs = internalTransactionSettings();

    TraceConfig specialties =
        traceConfigs.getTraceConfig(Collections.singletonList("http://localhost/specialties"));
    assertNotNull(specialties);
    assertEquals(0, specialties.getSampleRate());
    assertFalse(specialties.hasSampleStartFlag());

    assertNull(traceConfigs.getTraceConfig(Collections.singletonList("example.com/greet")));
  }

  @Test
  void testCreateWithEmptyConfigUsesDefaults() {
    tested.create(DeclarativeConfigProperties.empty());

    assertNull(samplingConfiguration().getTracingMode());
    assertNull(samplingConfiguration().getInternalTransactionSettings());
    assertTrue(samplingConfiguration().isTriggerTraceEnabled());
  }

  @Test
  void testCreateDisablesTriggerTrace() {
    tested.create(fromYaml("swo_http:\n  trigger_trace: false\n"));
    assertFalse(samplingConfiguration().isTriggerTraceEnabled());
  }

  @Test
  void testCreateAppliesUrlSampleRates() {
    tested.create(
        fromYaml(
            "swo_http:\n"
                + "  url_sample_rates:\n"
                + "    - .*/greet:\n"
                + "        sample_rate: 500000\n"
                + "    - .*/health:\n"
                + "        tracing_mode: never\n"));
    TraceConfigs traceConfigs = internalTransactionSettings();

    TraceConfig greet =
        traceConfigs.getTraceConfig(Collections.singletonList("http://example.com/greet"));
    assertEquals(500000, greet.getSampleRate());
    assertTrue(greet.hasSampleStartFlag());

    TraceConfig health =
        traceConfigs.getTraceConfig(Collections.singletonList("http://example.com/health"));
    assertEquals(0, health.getSampleRate());
    assertFalse(health.hasSampleStartFlag());
  }

  @Test
  void testCreateAppliesExtensionsMatcher() {
    tested.create(
        fromYaml(
            "swo_http:\n"
                + "  transaction_settings:\n"
                + "    - extensions: [png, jpg]\n"
                + "      tracing: disabled\n"));
    TraceConfigs traceConfigs = internalTransactionSettings();

    assertNotNull(
        traceConfigs.getTraceConfig(Collections.singletonList("http://example.com/logo.png")));
    assertNull(traceConfigs.getTraceConfig(Collections.singletonList("http://example.com/api")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidConfigs")
  void testCreateThrowsForInvalidConfig(String description, String yaml, String expectedMessage) {
    tested.create(fromYaml("swo_http:\n  tracing_mode: disabled\n  trigger_trace: false\n"));
    SamplingConfiguration previous = samplingConfiguration();

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> tested.create(fromYaml(yaml)));

    InvalidConfigException cause =
        assertInstanceOf(InvalidConfigException.class, exception.getCause());
    assertTrue(
        cause.getMessage().contains(expectedMessage),
        () ->
            "Expected message containing [" + expectedMessage + "] but was: " + cause.getMessage());
    assertSame(previous, samplingConfiguration());
  }

  static Stream<Arguments> invalidConfigs() {
    return Stream.of(
        Arguments.of(
            "unknown tracing_mode",
            "swo_http:\n  tracing_mode: sometimes\n",
            "Invalid tracing_mode : sometimes"),
        Arguments.of(
            "unknown transaction tracing value",
            "swo_http:\n  transaction_settings:\n    - regex: .*\n      tracing: maybe\n",
            "Invalid \"tracing\" value [maybe]"),
        Arguments.of(
            "transaction setting without tracing",
            "swo_http:\n  transaction_settings:\n    - regex: .*\n",
            "Need to define \"tracing\""),
        Arguments.of(
            "transaction setting without matcher",
            "swo_http:\n  transaction_settings:\n    - tracing: disabled\n",
            "Cannot find proper matcher"),
        Arguments.of(
            "transaction setting with multiple matchers",
            "swo_http:\n  transaction_settings:\n"
                + "    - regex: .*\n      extensions: [png]\n      tracing: disabled\n",
            "Multiple matchers found"),
        Arguments.of(
            "transaction setting with unknown key",
            "swo_http:\n  transaction_settings:\n"
                + "    - regex: .*\n      tracing: disabled\n      extra: extra\n",
            "Unknown key(s) [extra]"),
        Arguments.of(
            "transaction setting with invalid regex",
            "swo_http:\n  transaction_settings:\n    - regex: '[A-Za-z{'\n      tracing: disabled\n",
            "Failed to compile pattern [A-Za-z{"),
        Arguments.of(
            "url sample rate with invalid regex",
            "swo_http:\n  url_sample_rates:\n    - '[A-Za-z{':\n        sample_rate: 1\n",
            "Failed to compile the url sample rate of url pattern [[A-Za-z{]"),
        Arguments.of(
            "url sample rate without sample_rate or tracing_mode",
            "swo_http:\n  url_sample_rates:\n    - .*/greet:\n        other: 1\n",
            "Need to define either tracing_mode or sample_rate"),
        Arguments.of(
            "url sample rate with always and no sample_rate",
            "swo_http:\n  url_sample_rates:\n    - .*/greet:\n        tracing_mode: always\n",
            "Define sample_rate if tracing_mode is \"always\""));
  }

  private static SamplingConfiguration samplingConfiguration() {
    return SettingsManager.getSamplingConfiguration();
  }

  private static TraceConfigs internalTransactionSettings() {
    return samplingConfiguration().getInternalTransactionSettings();
  }

  private DeclarativeConfigProperties fromYaml(String yaml) {
    return DeclarativeConfiguration.toConfigProperties(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
        .getStructured("swo_http");
  }

  private DeclarativeConfigProperties loadConfig() {
    try (InputStream in = getClass().getResourceAsStream("/sdk-config.yaml")) {
      return DeclarativeConfiguration.toConfigProperties(in)
          .getStructured("tracer_provider", DeclarativeConfigProperties.empty())
          .getStructured("sampler", DeclarativeConfigProperties.empty())
          .getStructured("swo_http");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
