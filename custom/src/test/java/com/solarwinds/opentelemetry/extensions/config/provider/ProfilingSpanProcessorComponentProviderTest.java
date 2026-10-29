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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.config.JavaRuntimeVersionChecker;
import com.solarwinds.joboe.config.ProxyConfig;
import com.solarwinds.joboe.core.profiler.ProfilerSetting;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class ProfilingSpanProcessorComponentProviderTest {

  private static final Set<String> DEFAULT_EXCLUDES =
      new HashSet<>(Arrays.asList("java", "javax", "com.sun", "sun", "sunw"));

  private final ProfilingSpanProcessorComponentProvider tested =
      new ProfilingSpanProcessorComponentProvider();

  @AfterEach
  void tearDown() {
    ConfigManager.reset();
  }

  @Test
  void testName() {
    assertEquals("swo_profiler", tested.getName());
  }

  @Test
  void testConvert() throws InvalidConfigException {
    ProfilerSetting profilerSetting = tested.convert(loadProfilerConfig("/sdk-config.yaml"));
    assertEquals(new ProfilerSetting(true, DEFAULT_EXCLUDES, 20, 100, 2), profilerSetting);
  }

  @Test
  void testConvertDisablesProfilerWhenJdkUnsupported() throws InvalidConfigException {
    try (MockedStatic<JavaRuntimeVersionChecker> checker =
        Mockito.mockStatic(JavaRuntimeVersionChecker.class)) {
      checker.when(JavaRuntimeVersionChecker::isJdkVersionSupported).thenReturn(false);

      ProfilerSetting profilerSetting = tested.convert(loadProfilerConfig("/sdk-config.yaml"));
      assertEquals(new ProfilerSetting(false, DEFAULT_EXCLUDES, 20, 100, 2), profilerSetting);
    }
  }

  @Test
  void testConvertWithException() {
    DeclarativeConfigProperties config = loadProfilerConfig("/sdk-config-bad.yaml");
    assertThrows(InvalidConfigException.class, () -> tested.convert(config));
  }

  @Test
  void testCreateSetsProfilerConfig() {
    SpanProcessor processor = tested.create(loadProfilerConfig("/sdk-config.yaml"));

    assertNotNull(processor);
    assertEquals(
        new ProfilerSetting(true, DEFAULT_EXCLUDES, 20, 100, 2),
        ConfigManager.getConfig(ConfigProperty.PROFILER));
  }

  @Test
  void testCreateWithInvalidConfigThrows() {
    DeclarativeConfigProperties config = loadProfilerConfig("/sdk-config-bad.yaml");
    assertThrows(RuntimeException.class, () -> tested.create(config));
  }

  @Test
  void testConvertSetsProxyConfig() throws InvalidConfigException {
    tested.convert(fromYaml("swo_profiler:\n  proxy:\n    host: proxy.local\n    port: 3128\n"));

    ProxyConfig proxyConfig = (ProxyConfig) ConfigManager.getConfig(ConfigProperty.AGENT_PROXY);
    assertEquals("proxy.local", proxyConfig.getHost());
    assertEquals(3128, proxyConfig.getPort());
  }

  @Test
  void testConvertWithoutProxyLeavesProxyConfigUnset() throws InvalidConfigException {
    tested.convert(loadProfilerConfig("/sdk-config.yaml"));
    assertNull(ConfigManager.getConfig(ConfigProperty.AGENT_PROXY));
  }

  @Test
  void testConvertWithIncompleteProxyLeavesProxyConfigUnset() throws InvalidConfigException {
    tested.convert(fromYaml("swo_profiler:\n  proxy:\n    host: proxy.local\n"));
    assertNull(ConfigManager.getConfig(ConfigProperty.AGENT_PROXY));
  }

  private DeclarativeConfigProperties fromYaml(String yaml) {
    return DeclarativeConfiguration.toConfigProperties(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
        .getStructured("swo_profiler");
  }

  private DeclarativeConfigProperties loadProfilerConfig(String resource) {
    try (InputStream resourceAsStream =
        ProfilingSpanProcessorComponentProviderTest.class.getResourceAsStream(resource)) {
      return findProfilerConfig(resourceAsStream, resource);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private DeclarativeConfigProperties findProfilerConfig(InputStream in, String name) {
    return DeclarativeConfiguration.toConfigProperties(in)
        .getStructured("tracer_provider", DeclarativeConfigProperties.empty())
        .getStructuredList("processors", Collections.emptyList())
        .stream()
        .map(processor -> processor.getStructured("swo_profiler"))
        .filter(Objects::nonNull)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("swo_profiler not found in " + name));
  }
}
