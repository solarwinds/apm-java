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

package com.solarwinds.opentelemetry.instrumentation.feature.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.solarwinds.joboe.core.settings.TestSettingsReader;
import com.solarwinds.joboe.core.util.TestUtils;
import com.solarwinds.joboe.sampling.SampleRateSource;
import com.solarwinds.joboe.sampling.SamplingConfiguration;
import com.solarwinds.joboe.sampling.SettingsManager;
import com.solarwinds.joboe.sampling.TraceConfig;
import com.solarwinds.joboe.sampling.TraceConfigs;
import com.solarwinds.joboe.sampling.TracingMode;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.UrlAttributes;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class TransactionFilteringTest {

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  @BeforeEach
  void setUp() {
    Map<com.solarwinds.joboe.sampling.ResourceMatcher, TraceConfig> urlConfigs =
        new LinkedHashMap<>();
    urlConfigs.put(
        url -> url.contains("/specialties"),
        new TraceConfig(0, SampleRateSource.FILE, TracingMode.DISABLED.toFlags()));

    TestSettingsReader reader = TestUtils.initSettingsReader();
    reader.put(
        new TestSettingsReader.SettingsMockupBuilder()
            .withFlags(true, false, true, true, false)
            .withSampleRate(1_000_000)
            .build());

    SettingsManager.initialize(
        new com.solarwinds.joboe.core.settings.SimpleSettingsFetcher(reader),
        SamplingConfiguration.builder()
            .internalTransactionSettings(new TraceConfigs(urlConfigs))
            .build());
  }

  @AfterEach
  void tearDown() {
    TestSettingsReader reader = TestUtils.initSettingsReader();
    reader.put(
        new TestSettingsReader.SettingsMockupBuilder()
            .withFlags(true, false, true, true, false)
            .withSampleRate(1_000_000)
            .build());
  }

  @Test
  void verifyFilteredUrlIsNotSampled() {
    Tracer tracer = GlobalOpenTelemetry.get().getTracer("test");
    Span span =
        tracer
            .spanBuilder("GET /specialties")
            .setSpanKind(SpanKind.SERVER)
            .setAttribute(UrlAttributes.URL_PATH, "/petclinic/api/specialties")
            .startSpan();
    span.end();

    await()
        .during(1, TimeUnit.SECONDS)
        .atMost(3, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(testing.waitForTraces(0)).isEmpty());
  }

  @Test
  void verifyNonFilteredUrlIsSampled() {
    Tracer tracer = GlobalOpenTelemetry.get().getTracer("test");
    Span span =
        tracer
            .spanBuilder("GET /owners")
            .setSpanKind(SpanKind.SERVER)
            .setAttribute(UrlAttributes.URL_PATH, "/petclinic/api/owners")
            .startSpan();

    span.end();
    List<List<SpanData>> traces = testing.waitForTraces(1);
    assertThat(traces).hasSize(1);
  }
}
