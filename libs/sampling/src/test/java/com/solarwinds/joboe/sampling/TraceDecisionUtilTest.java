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

package com.solarwinds.joboe.sampling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TraceDecisionUtilTest {
  private static final String TEST_LAYER = "test";

  private static final String X_TRACE_ID_SAMPLED =
      MetadataTest.getXTraceid(Metadata.CURRENT_VERSION, true);
  private static final String X_TRACE_ID_NOT_SAMPLED =
      MetadataTest.getXTraceid(Metadata.CURRENT_VERSION, false);
  private static final String X_TRACE_ID_INCOMPATIBLE =
      MetadataTest.getXTraceid(Metadata.CURRENT_VERSION + 1, true);
  private static final String X_TRACE_ID_ALL_ZEROS = new Metadata().toHexString();
  private static final String X_TRACE_ID_INCORRECT_FORMAT = "XYZ";

  private static final XtraceOptions TRIGGER_TRACE_OPTIONS =
      XtraceOptions.getXTraceOptions("trigger-trace", null);

  @Mock private SettingsFetcher settingsFetcherMock;

  @BeforeEach
  void setup() {
    SettingsManager.initialize(settingsFetcherMock, SamplingConfiguration.builder().build());
    TraceDecisionUtil.reset();
  }

  @Test
  public void testShouldTraceRequest() throws Exception {
    // tracing mode NEVER
    when(settingsFetcherMock.getSettings())
        .thenReturn(SettingsStub.builder().withFlags(false, false, false, false, true).build());

    assertFalse(TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, null, null, null).isSampled());
    assertFalse(TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, null, null, null).isSampled());
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_SAMPLED, null, null)
            .isSampled());
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_NOT_SAMPLED, null, null)
            .isSampled());
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_INCOMPATIBLE, null, null)
            .isSampled());
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_ALL_ZEROS, null, null)
            .isSampled());
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_INCORRECT_FORMAT, null, null)
            .isSampled());

    // tracing mode ALWAYS
    when(settingsFetcherMock.getSettings())
        .thenReturn(SettingsStub.builder().withFlags(true, false, true, true, true).build());

    assertTrue(TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, null, null, null).isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_SAMPLED, null, null)
            .isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, null, TRIGGER_TRACE_OPTIONS, null)
            .isSampled());
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_NOT_SAMPLED, null, null)
            .isSampled()); // if upstream decides to not sample this, then it should not continue
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_INCOMPATIBLE, null, null)
            .isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_ALL_ZEROS, null, null)
            .isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_INCORRECT_FORMAT, null, null)
            .isSampled());

    // tracing mode THROUGH_ALWAYS
    when(settingsFetcherMock.getSettings())
        .thenReturn(SettingsStub.builder().withFlags(false, false, true, false, true).build());

    assertFalse(TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, null, null, null).isSampled());
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, null, TRIGGER_TRACE_OPTIONS, null)
            .isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_SAMPLED, null, null)
            .isSampled());
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_NOT_SAMPLED, null, null)
            .isSampled()); // if upstream decides to not sample this, then it should not continue
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_INCOMPATIBLE, null, null)
            .isSampled()); // incompatible x-trace header, so x-trace id is ignored
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_ALL_ZEROS, null, null)
            .isSampled()); // invalid x-trace header, so x-trace id is ignored
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_INCORRECT_FORMAT, null, null)
            .isSampled()); // incorrect format x-trace header, so x-trace id is ignored
  }

  @Test
  public void testNoSettings() throws Exception {
    when(settingsFetcherMock.getSettings())
        .thenReturn(SettingsStub.builder().withFlags(false, false, false, false, true).build());

    // do not trace in any situation if settings not available
    // https://tracelytics.atlassian.net/browse/TVI-1588
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest("NotFoundLayer", null, null, null).isSampled());
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest("NotFoundLayer", X_TRACE_ID_SAMPLED, null, null)
            .isSampled());
  }

  @Test
  public void testPrecedence() throws Exception {
    when(settingsFetcherMock.getSettings())
        .thenReturn(SettingsStub.builder().withFlags(false, false, false, false, true).build());

    // case 1: set remote TracingMode = ENABLED with no override, no local settings
    // local universal : null/null
    // remote : ENABLED/100% (override OFF)
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSettingsType(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE)
                .build());
    assertTrue(TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, null, null, null).isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_SAMPLED, null, null)
            .isSampled());

    // case 2: set remote TracingMode = ENABLED with override, no local settings
    // local universal : null/null
    // remote : ENABLED/100%
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, true)
                .withSettingsType(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE)
                .build());
    assertTrue(TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, null, null, null).isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_SAMPLED, null, null)
            .isSampled());

    // case 3: set local TracingMode = ENABLED with no sample rate
    // local universal : ENABLED/null
    // remote : ENABLED/100%
    SettingsManager.initialize(
        settingsFetcherMock,
        SamplingConfiguration.builder().tracingMode(TracingMode.ENABLED).build());
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, true)
                .withSettingsType(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE)
                .build());

    // should still trace
    assertTrue(TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, null, null, null).isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_SAMPLED, null, null)
            .isSampled());

    // case 4: set no local TracingMode, with sample rate = 0%
    // local universal : null/0%
    // remote : ENABLED/100%
    SettingsManager.initialize(
        settingsFetcherMock, SamplingConfiguration.builder().sampleRate(0).build());
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, true)
                .withSettingsType(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE)
                .build());
    // should not start trace
    assertFalse(TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, null, null, null).isSampled());
    // but it should continue trace
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_SAMPLED, null, null)
            .isSampled());

    // case 5: set local TracingMode = DISABLED, with no sample Rate
    // local universal : DISABLED/null
    // remote : ENABLED/100%
    SettingsManager.initialize(
        settingsFetcherMock,
        SamplingConfiguration.builder().tracingMode(TracingMode.DISABLED).build());
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, true)
                .withSettingsType(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE)
                .build());
    // should not trace
    assertFalse(TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, null, null, null).isSampled());
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, X_TRACE_ID_SAMPLED, null, null)
            .isSampled());

    // case 6: add URL settings with Sample rate = 1000000
    // local URL : ENBALED/100%
    // local universal : DISABLED/null
    // remote : ENABLED/100%
    TraceConfigs testingUrlSampleRateConfigs =
        buildUrlConfigs(url -> true, TracingMode.ALWAYS, 1000000);
    SettingsManager.initialize(
        settingsFetcherMock,
        SamplingConfiguration.builder()
            .internalTransactionSettings(testingUrlSampleRateConfigs)
            .tracingMode(TracingMode.DISABLED)
            .build());

    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, true)
                .withSettingsType(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE)
                .build());

    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER, null, null, Collections.singletonList("http://something.html"))
            .isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER,
                X_TRACE_ID_SAMPLED,
                null,
                Collections.singletonList("http://something.html"))
            .isSampled());

    // case 7: add transaction settings with Trace modes with no sample rate
    // local transaction settings (*.png, *.jpg) : DISABLED/0%
    // local transaction settings (*.trace.*) : ENABLED/100%
    // local universal : DISABLED/null
    // remote : ENABLED/100%
    Map<ResourceMatcher, TraceConfig> urlTraceConfigsByMatcher = new LinkedHashMap<>();
    urlTraceConfigsByMatcher.put(
        url -> url.endsWith("png") || url.endsWith("jpg"),
        buildTraceConfig(TracingMode.DISABLED, 0));
    urlTraceConfigsByMatcher.put(
        url -> url.contains("trace"), buildTraceConfig(TracingMode.ENABLED, null));
    SettingsManager.initialize(
        settingsFetcherMock,
        SamplingConfiguration.builder()
            .internalTransactionSettings(new TraceConfigs(urlTraceConfigsByMatcher))
            .tracingMode(TracingMode.DISABLED)
            .build());

    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, true)
                .withSettingsType(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE)
                .build());

    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER, null, null, Collections.singletonList("http://something.png"))
            .isSampled());
    assertEquals(
        SampleRateSource.FILE,
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER, null, null, Collections.singletonList("http://something.png"))
            .getTraceConfig()
            .getSampleRateSource());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER, null, null, Collections.singletonList("http://trace-this"))
            .isSampled());
    assertEquals(
        SampleRateSource.OBOE,
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER, null, null, Collections.singletonList("http://trace-this"))
            .getTraceConfig()
            .getSampleRateSource()); // source is from OBOE as rate is NOT defined in local config
  }

  @Test
  public void testUrlConfigs() throws Exception {
    // Add SRv1 always, override, sampling rate 1000000
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, true)
                .withSampleRate(1000000)
                .withSettingsType(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE)
                .build());

    assertEquals(1000000, TraceDecisionUtil.getRemoteTraceConfig().getSampleRate());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER, null, null, Collections.singletonList("http://something.html"))
            .isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER,
                X_TRACE_ID_SAMPLED,
                null,
                Collections.singletonList("http://something.html"))
            .isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER, null, null, Collections.singletonList("http://something.html"))
            .isReportMetrics());

    // Add local URL rate, should override the SRv1 rate if pattern matches
    TraceConfigs testingUrlSampleRateConfigs =
        buildUrlConfigs(url -> url.endsWith(".html"), TracingMode.ALWAYS, 0);
    SettingsManager.initialize(
        settingsFetcherMock,
        SamplingConfiguration.builder()
            .internalTransactionSettings(testingUrlSampleRateConfigs)
            .build());

    // pattern match, should all have sample rate 0% for new traces, but continuing/AVW trace should
    // still go on
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER, null, null, Collections.singletonList("http://something.html"))
            .isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER,
                X_TRACE_ID_SAMPLED,
                null,
                Collections.singletonList("http://something.html"))
            .isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER, null, null, Collections.singletonList("http://something.html"))
            .isReportMetrics()); // metrics should still be reported even for 0% rate

    // pattern not match, take the Srv1 override with sample rate 100%
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER, null, null, Collections.singletonList("http://something.xxx"))
            .isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER,
                X_TRACE_ID_SAMPLED,
                null,
                Collections.singletonList("http://something.xxx"))
            .isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER, null, null, Collections.singletonList("http://something.xxx"))
            .isReportMetrics());

    // Add local URL rate, should override the SRv1 rate if pattern matches
    testingUrlSampleRateConfigs =
        buildUrlConfigs(url -> url.endsWith(".html"), TracingMode.NEVER, 0);
    SettingsManager.initialize(
        settingsFetcherMock,
        SamplingConfiguration.builder()
            .internalTransactionSettings(testingUrlSampleRateConfigs)
            .build());

    // pattern match, should block all traffic even for continuing traces since the url tracingMode
    // is never
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER, null, null, Collections.singletonList("http://something.html"))
            .isSampled());
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER,
                X_TRACE_ID_SAMPLED,
                null,
                Collections.singletonList("http://something.html"))
            .isSampled());
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER,
                X_TRACE_ID_SAMPLED,
                null,
                Collections.singletonList("http://something.html"))
            .isReportMetrics()); // trace mode never disables metrics too

    // pattern not match, take the Srv1 override with sample rate 100%
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER, null, null, Collections.singletonList("http://something.xxx"))
            .isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER,
                X_TRACE_ID_SAMPLED,
                null,
                Collections.singletonList("http://something.xxx"))
            .isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER,
                X_TRACE_ID_SAMPLED,
                null,
                Collections.singletonList("http://something.xxx"))
            .isReportMetrics());

    Map<ResourceMatcher, TraceConfig> urlTraceConfigsByMatcher = new LinkedHashMap<>();
    urlTraceConfigsByMatcher.put(
        url -> url.endsWith("png") || url.endsWith("jpg"),
        buildTraceConfig(TracingMode.DISABLED, 0));
    urlTraceConfigsByMatcher.put(
        url -> url.contains("trace"), buildTraceConfig(TracingMode.ENABLED, null));
    SettingsManager.initialize(
        settingsFetcherMock,
        SamplingConfiguration.builder()
            .internalTransactionSettings(new TraceConfigs(urlTraceConfigsByMatcher))
            .build());
    TraceDecision traceDecision;

    // pattern match on "disabled", should block all traffic even for continuing traces since the
    // transaction settings tracingMode is disabled
    traceDecision =
        TraceDecisionUtil.shouldTraceRequest(
            TEST_LAYER, null, null, Collections.singletonList("http://something.png"));
    assertFalse(traceDecision.isSampled());
    assertEquals(
        0,
        traceDecision
            .getTraceConfig()
            .getSampleRate()); // rate is coming from the local transaction settings
    assertEquals(
        SampleRateSource.FILE,
        traceDecision
            .getTraceConfig()
            .getSampleRateSource()); // rate is coming from the local transaction settings
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER,
                X_TRACE_ID_SAMPLED,
                null,
                Collections.singletonList("http://something.png"))
            .isSampled());
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER,
                X_TRACE_ID_SAMPLED,
                null,
                Collections.singletonList("http://something.png"))
            .isReportMetrics()); // trace mode never disables metrics too

    // pattern match on "enabled", take the flags from local config but sample rate from remote
    // settings
    traceDecision =
        TraceDecisionUtil.shouldTraceRequest(
            TEST_LAYER, null, null, Collections.singletonList("http://trace.com"));
    assertTrue(traceDecision.isSampled());
    assertEquals(
        1000000,
        traceDecision.getTraceConfig().getSampleRate()); // rate is coming from the remote settings
    assertEquals(
        SampleRateSource.OBOE,
        traceDecision
            .getTraceConfig()
            .getSampleRateSource()); // rate is coming from the remote settings
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER, X_TRACE_ID_SAMPLED, null, Collections.singletonList("http://trace.com"))
            .isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER, X_TRACE_ID_SAMPLED, null, Collections.singletonList("http://trace.com"))
            .isReportMetrics());

    // pattern not match, take the Srv1 override with sample rate 100%
    traceDecision =
        TraceDecisionUtil.shouldTraceRequest(
            TEST_LAYER, null, null, Collections.singletonList("http://something.xxx"));
    assertTrue(traceDecision.isSampled());
    assertEquals(
        1000000,
        traceDecision.getTraceConfig().getSampleRate()); // rate is coming from the remote settings
    assertEquals(
        SampleRateSource.OBOE,
        traceDecision
            .getTraceConfig()
            .getSampleRateSource()); // rate is coming from the remote settings
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER,
                X_TRACE_ID_SAMPLED,
                null,
                Collections.singletonList("http://something.xxx"))
            .isSampled());
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                TEST_LAYER,
                X_TRACE_ID_SAMPLED,
                null,
                Collections.singletonList("http://something.xxx"))
            .isReportMetrics());
  }

  @Test
  public void testThroughput() throws Exception {
    when(settingsFetcherMock.getSettings())
        .thenReturn(SettingsStub.builder().withFlags(true, false, true, true, false).build());

    int data;
    TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.THROUGHPUT); // clear it

    TraceDecisionUtil.shouldTraceRequest("LayerA", null, null, null);
    TraceDecisionUtil.shouldTraceRequest("LayerA", null, null, null);
    TraceDecisionUtil.shouldTraceRequest(
        "LayerC", X_TRACE_ID_SAMPLED, null, null); // Continue trace, should also count

    data = TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.THROUGHPUT);

    assertEquals(3, data);

    data =
        TraceDecisionUtil.consumeMetricsData(
            TraceDecisionUtil.MetricType
                .THROUGHPUT); // consumed once already, so this time should return 0
    assertEquals(0, data);
  }

  @Test
  public void testThroughputConcurrency() throws Exception {
    when(settingsFetcherMock.getSettings())
        .thenReturn(SettingsStub.builder().withFlags(true, false, true, true, false).build());

    TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.THROUGHPUT); // clear it

    final int THREAD_COUNT = 1000;
    final int RUN_PER_THREAD = 100;

    ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
    List<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
    for (int i = 0; i < THREAD_COUNT; i++) {
      tasks.add(
          () -> {
            for (int i1 = 0; i1 < RUN_PER_THREAD; i1++) {
              TraceDecisionUtil.shouldTraceRequest("LayerA", null, null, null);
            }
            return null;
          });
    }

    executorService.invokeAll(tasks);

    int data = TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.THROUGHPUT);
    assertEquals(THREAD_COUNT * RUN_PER_THREAD, data);

    // try to do increment and clear at once
    tasks.clear();
    for (int i = 0; i < THREAD_COUNT; i++) {
      tasks.add(
          () -> {
            TraceDecisionUtil.shouldTraceRequest("LayerA", null, null, null);
            int data1 =
                TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.THROUGHPUT);
            return data1;
          });
    }

    executorService.invokeAll(
        tasks); // do not really need to assert, we just want to make sure no exceptions is
    // triggered. The number could be a bit off and it's acceptable
    executorService.shutdown();
    executorService.awaitTermination(10, TimeUnit.SECONDS);
  }

  @Test
  public void testTokenBucketExhaustion() throws Exception {
    TraceDecisionUtil.consumeMetricsData(
        TraceDecisionUtil.MetricType.TOKEN_BUCKET_EXHAUSTION); // clear it
    SettingsManager.initialize(settingsFetcherMock, SamplingConfiguration.builder().build());

    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSampleRate(1000000)
                .withSettingsArg(SettingsArg.BUCKET_CAPACITY, 0.0)
                .withSettingsArg(SettingsArg.BUCKET_RATE, 0.0)
                .build());
    TraceDecisionUtil.shouldTraceRequest("LayerA", null, null, null); // exhaustion +1
    TraceDecisionUtil.shouldTraceRequest("LayerA", null, null, null); // exhaustion +1
    TraceDecisionUtil.shouldTraceRequest(
        "LayerA",
        X_TRACE_ID_SAMPLED,
        null,
        null); // no change in exhaustion, as Continue trace does not have bucket restriction
    // LayerA should have 2 token bucket exhaustion count

    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSampleRate(0)
                .withSettingsArg(SettingsArg.BUCKET_CAPACITY, 0.0)
                .withSettingsArg(SettingsArg.BUCKET_RATE, 0.0)
                .build());
    TraceDecisionUtil.shouldTraceRequest(
        "LayerB", null, null, null); // no change in exhaustion, sample rate at 0
    TraceDecisionUtil.shouldTraceRequest(
        "LayerB", null, null, null); // no change in exhaustion, sample rate at 0
    TraceDecisionUtil.shouldTraceRequest(
        "LayerB",
        X_TRACE_ID_SAMPLED,
        null,
        null); // no change in exhaustion, as Continue trace does not have bucket restriction
    // LayerB should not appear in the map as it has no exhaustion count

    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSampleRate(1000000)
                .withSettingsArg(SettingsArg.BUCKET_CAPACITY, 1.0)
                .withSettingsArg(SettingsArg.BUCKET_RATE, 0.0)
                .build());
    TraceDecisionUtil.shouldTraceRequest(
        "LayerC", null, null, null); // exhaustion +1, sharing same token bucket as LayerA
    TraceDecisionUtil.shouldTraceRequest(
        "LayerC", null, null, null); // exhaustion +1, tokens all used up already
    TraceDecisionUtil.shouldTraceRequest(
        "LayerC",
        X_TRACE_ID_SAMPLED,
        null,
        null); // no change in exhaustion, as Continue trace does not have bucket restriction
    // LayerC should have 1 token bucket exhaustion count

    int data =
        TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.TOKEN_BUCKET_EXHAUSTION);
    assertEquals(4, data);

    data =
        TraceDecisionUtil.consumeMetricsData(
            TraceDecisionUtil.MetricType
                .TOKEN_BUCKET_EXHAUSTION); // consumed once already, so this time should return
    // empty map
    assertEquals(0, data);

    // test signed/trigger trace requests
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSampleRate(1000000)
                .withSettingsArg(SettingsArg.BUCKET_CAPACITY, 0.0)
                .withSettingsArg(SettingsArg.BUCKET_RATE, 0.0)
                .withSettingsArg(SettingsArg.STRICT_BUCKET_CAPACITY, 1.0)
                .withSettingsArg(SettingsArg.STRICT_BUCKET_RATE, 0.0)
                .withSettingsArg(SettingsArg.RELAXED_BUCKET_CAPACITY, 2.0)
                .withSettingsArg(SettingsArg.RELAXED_BUCKET_RATE, 0.0)
                .build());
    TraceDecisionUtil.shouldTraceRequest("LayerA", null, null, null); // exhaustion +1
    TraceDecisionUtil.shouldTraceRequest("LayerA", null, null, null); // exhaustion +1
    TraceDecisionUtil.shouldTraceRequest("LayerA", null, null, null); // exhaustion +1

    TraceDecisionUtil.shouldTraceRequest("LayerA", null, TRIGGER_TRACE_OPTIONS, null); // ok
    TraceDecisionUtil.shouldTraceRequest(
        "LayerA", null, TRIGGER_TRACE_OPTIONS, null); // exhaustion +1
    TraceDecisionUtil.shouldTraceRequest(
        "LayerA", null, TRIGGER_TRACE_OPTIONS, null); // exhaustion +1

    XtraceOptions goodSignatureWithTriggerTraceOptions =
        new XtraceOptions(
            Collections.singletonMap(XtraceOption.TRIGGER_TRACE, true),
            Collections.emptyList(),
            XtraceOptions.AuthenticationStatus.OK);
    TraceDecisionUtil.shouldTraceRequest(
        "LayerA", null, goodSignatureWithTriggerTraceOptions, null); // ok
    TraceDecisionUtil.shouldTraceRequest(
        "LayerA", null, goodSignatureWithTriggerTraceOptions, null); // ok
    TraceDecisionUtil.shouldTraceRequest(
        "LayerA", null, goodSignatureWithTriggerTraceOptions, null); // exhaustion +1

    data =
        TraceDecisionUtil.consumeMetricsData(
            TraceDecisionUtil.MetricType
                .TOKEN_BUCKET_EXHAUSTION); // consumed once already, so this time should return
    // empty map
    assertEquals(6, data);
  }

  @Test
  public void testTokenBucket() throws Exception {
    when(settingsFetcherMock.getSettings())
        .thenReturn(SettingsStub.builder().withFlags(true, false, true, true, false).build());

    String bucketLayer = "test";

    // bucket capacity at 100, rate at 100 trace per sec
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, true, true, true, true)
                .withSampleRate(1000000)
                .withSettingsArg(SettingsArg.BUCKET_CAPACITY, 30.0)
                .withSettingsArg(SettingsArg.BUCKET_RATE, 0.0)
                .build());

    TraceConfig config =
        TraceDecisionUtil.shouldTraceRequest(
                bucketLayer, null, null, Collections.singletonList("http://something.html"))
            .getTraceConfig(); // tracing with token
    assertNotNull(config);
    assertEquals(30.0, config.getBucketCapacity(TokenBucketType.REGULAR));
    assertEquals(0.0, config.getBucketRate(TokenBucketType.REGULAR));
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                bucketLayer,
                X_TRACE_ID_SAMPLED,
                null,
                Collections.singletonList("http://something.html"))
            .isSampled()); // continue trace not restricted by token bucket

    // bucket capacity at 0, rate at 100 trace per sec
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, true, true, true, true)
                .withSampleRate(1000000)
                .withSettingsArg(SettingsArg.BUCKET_CAPACITY, 0.0)
                .withSettingsArg(SettingsArg.BUCKET_RATE, 100.0)
                .build());

    TimeUnit.SECONDS.sleep(1);
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(
                bucketLayer, null, null, Collections.singletonList("http://something.html"))
            .isSampled()); // no new trace as capacity is at zero
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                bucketLayer,
                X_TRACE_ID_SAMPLED,
                null,
                Collections.singletonList("http://something.html"))
            .isSampled()); // continue trace not restricted by token bucket

    // bucket capacity at 50, rate at 0 trace per sec
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, true, true, true, true)
                .withSampleRate(1000000)
                .withSettingsArg(SettingsArg.BUCKET_CAPACITY, 50.0)
                .withSettingsArg(SettingsArg.BUCKET_RATE, 0.0)
                .build());
    TimeUnit.SECONDS.sleep(1);
    assertFalse(
        TraceDecisionUtil.shouldTraceRequest(
                bucketLayer, null, null, Collections.singletonList("http://something.html"))
            .isSampled()); // not tracing, sharing the same bucket instance, it has capacity 50 now
    // but zero replenish rate and 0 left-over token from previous capacity
    // which is zero

    // bucket capacity at 50, rate at 100 trace per sec, should trace again
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, true, true, true, true)
                .withSampleRate(1000000)
                .withSettingsArg(SettingsArg.BUCKET_CAPACITY, 50.0)
                .withSettingsArg(SettingsArg.BUCKET_RATE, 100.0)
                .build());
    TimeUnit.SECONDS.sleep(1);
    assertTrue(
        TraceDecisionUtil.shouldTraceRequest(
                bucketLayer, null, null, Collections.singletonList("http://something.html"))
            .isSampled()); // not tracing, sharing the same bucket instance, it has capacity 50 now
    // but zero replenish rate and 0 left-over token from previous capacity
    // which is zero
  }

  @Test
  public void testGetTokenBucket() throws Exception {
    assertTrue(
        TraceDecisionUtil.getTokenBucket(TokenBucketType.REGULAR, 1.0, 0.0)
            .consume()); // first time returns true 1 token consumed
    assertFalse(
        TraceDecisionUtil.getTokenBucket(TokenBucketType.REGULAR, 1.0, 0.0)
            .consume()); // second time returns false, no token remains

    // try RELAXED bucket type
    assertTrue(
        TraceDecisionUtil.getTokenBucket(TokenBucketType.RELAXED, 1.0, 0.0)
            .consume()); // first time returns true 1 token consumed
    assertFalse(
        TraceDecisionUtil.getTokenBucket(TokenBucketType.RELAXED, 1.0, 0.0)
            .consume()); // second time returns false, no token remains

    // try STRICT bucket type
    assertTrue(
        TraceDecisionUtil.getTokenBucket(TokenBucketType.STRICT, 1.0, 0.0)
            .consume()); // first time returns true 1 token consumed
    assertFalse(
        TraceDecisionUtil.getTokenBucket(TokenBucketType.STRICT, 1.0, 0.0)
            .consume()); // second time returns false, no token remains

    // try the REGULAR type again
    assertFalse(
        TraceDecisionUtil.getTokenBucket(TokenBucketType.REGULAR, 1.0, 0.0)
            .consume()); // still no token remains
  }

  @Test
  public void testTriggerTraceTraceDecision() throws Exception {
    TraceDecision result;
    // trigger trace enabled is set to true, tracing rate 100%
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSampleRate(1000000)
                .build());
    result = TraceDecisionUtil.shouldTraceRequest("LayerA", null, TRIGGER_TRACE_OPTIONS, null);
    assertTrue(result.isSampled()); // tracing, trigger trace flagged and enabled
    assertTrue(result.isReportMetrics()); // metric should be reported regardless of rate

    // trigger trace enabled is set to true, tracing rate 0
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSampleRate(0)
                .build());
    result = TraceDecisionUtil.shouldTraceRequest("LayerA", null, TRIGGER_TRACE_OPTIONS, null);
    assertTrue(result.isSampled()); // tracing, trigger trace flagged and enabled
    assertTrue(result.isReportMetrics()); // metric should be reported regardless of rate

    // trigger trace enabled is set to false, tracing rate 0
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, false, false)
                .withSampleRate(0)
                .build());
    result = TraceDecisionUtil.shouldTraceRequest("LayerA", null, TRIGGER_TRACE_OPTIONS, null);
    assertFalse(result.isSampled()); // not tracing, trigger trace flagged but disabled
    assertTrue(result.isReportMetrics()); // metric should be reported regardless of rate

    // trigger trace enabled is set to false, tracing rate 100%
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, false, false)
                .withSampleRate(1000000)
                .build());
    result = TraceDecisionUtil.shouldTraceRequest("LayerA", null, TRIGGER_TRACE_OPTIONS, null);
    assertFalse(result.isSampled()); // not tracing, trigger trace flagged but disabled
    assertTrue(result.isReportMetrics()); // metric should be reported regardless of rate

    // trigger trace enabled is set to false, tracing mode disabled
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(false, false, false, false, false)
                .withSampleRate(0)
                .build());
    result = TraceDecisionUtil.shouldTraceRequest("LayerA", null, TRIGGER_TRACE_OPTIONS, null);
    assertFalse(
        result.isSampled()); // not tracing, tracing mode disabled (so is the trigger trace option)
    assertFalse(result.isReportMetrics()); // no metrics, tracing mode disabled

    // trigger trace enabled is set to true, tracing rate 100% - bad signature
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSampleRate(1000000)
                .build());
    XtraceOptions badSignatureOptions =
        new XtraceOptions(
            Collections.emptyMap(),
            Collections.emptyList(),
            XtraceOptions.AuthenticationStatus.failure("bad-signature"));
    result = TraceDecisionUtil.shouldTraceRequest("LayerA", null, badSignatureOptions, null);
    assertFalse(result.isSampled()); // bad signature, no tracing
    assertTrue(
        result
            .isReportMetrics()); // metric should still be reported as bad signature does not affect
    // metrics reporting

    // trigger trace enabled is set to disable, trace mode disabled - bad signature
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(false, false, false, false, false)
                .withSampleRate(0)
                .build());
    result = TraceDecisionUtil.shouldTraceRequest("LayerA", null, badSignatureOptions, null);
    assertFalse(result.isSampled()); // bad signature, no tracing
    assertFalse(
        result.isReportMetrics()); // metric should not be reported due to trace mode disabled
  }

  @Test
  public void testTraceCount() throws Exception {
    when(settingsFetcherMock.getSettings())
        .thenReturn(SettingsStub.builder().withFlags(true, false, true, true, false).build());

    int data;
    TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.TRACE_COUNT); // clear it

    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSampleRate(1000000)
                .build()); // ALWAYS sample rate = 100%
    TraceDecisionUtil.shouldTraceRequest(
        "LayerA", null, null, null); // new trace at 100%, should count
    TraceDecisionUtil.shouldTraceRequest(
        "LayerA", X_TRACE_ID_SAMPLED, null, null); // Continue trace, should count
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSampleRate(0)
                .build()); // ALWAYS. sample rate = 0%
    TraceDecisionUtil.shouldTraceRequest(
        "LayerB", null, null, null); // new trace at 0%, should not count
    TraceDecisionUtil.shouldTraceRequest(
        "LayerB", X_TRACE_ID_SAMPLED, null, null); // Continue trace, should count
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(false, false, true, false, false)
                .withSampleRate(1000000)
                .build()); // THROUGH. sample rate = 100% (not used anyway for THROUGH traces)
    TraceDecisionUtil.shouldTraceRequest(
        "LayerC", null, null, null); // new trace at THROUGH mode, should not count
    TraceDecisionUtil.shouldTraceRequest(
        "LayerC", X_TRACE_ID_SAMPLED, null, null); // Continue trace, should count

    data = TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.TRACE_COUNT);

    assertEquals(4, data);
  }

  @Test
  public void testSampleCount() throws Exception {
    when(settingsFetcherMock.getSettings())
        .thenReturn(SettingsStub.builder().withFlags(true, false, true, true, false).build());

    int data;
    TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.SAMPLE_COUNT); // clear it

    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSampleRate(1000000)
                .build()); // ALWAYS sample rate = 100%
    TraceDecisionUtil.shouldTraceRequest("LayerA", null, null, null); // new trace at 100%, sampled
    TraceDecisionUtil.shouldTraceRequest(
        "LayerA", X_TRACE_ID_SAMPLED, null, null); // Continue trace, no sampling
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSampleRate(500000)
                .build()); // ALWAYS. sample rate = 50%
    TraceDecisionUtil.shouldTraceRequest("LayerB", null, null, null); // new trace at 0%, sampled
    TraceDecisionUtil.shouldTraceRequest(
        "LayerB", X_TRACE_ID_SAMPLED, null, null); // Continue trace, no sampling
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(false, false, true, false, false)
                .withSampleRate(1000000)
                .build()); // THROUGH. sample rate = 100% (not used anyway for THROUGH traces)
    TraceDecisionUtil.shouldTraceRequest(
        "LayerC", null, null, null); // new trace at THROUGH mode, no sampling
    TraceDecisionUtil.shouldTraceRequest(
        "LayerC", X_TRACE_ID_SAMPLED, null, null); // Continue trace, no sampling

    data = TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.SAMPLE_COUNT);

    assertEquals(2, data);
  }

  @Test
  public void testThroughTraceCount() throws Exception {
    when(settingsFetcherMock.getSettings())
        .thenReturn(SettingsStub.builder().withFlags(true, false, true, true, false).build());

    int data;
    TraceDecisionUtil.consumeMetricsData(
        TraceDecisionUtil.MetricType.THROUGH_TRACE_COUNT); // clear it

    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSampleRate(1000000)
                .build()); // ALWAYS sample rate = 100%
    TraceDecisionUtil.shouldTraceRequest("LayerA", null, null, null); // not through trace
    TraceDecisionUtil.shouldTraceRequest(
        "LayerA", X_TRACE_ID_SAMPLED, null, null); // THROUGH_TRACE_COUNT +1
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSampleRate(500000)
                .build()); // ALWAYS. sample rate = 50%
    TraceDecisionUtil.shouldTraceRequest("LayerB", null, null, null); // not through trace
    TraceDecisionUtil.shouldTraceRequest(
        "LayerB", X_TRACE_ID_SAMPLED, null, null); // THROUGH_TRACE_COUNT +1
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(false, false, true, false, false)
                .withSampleRate(1000000)
                .build()); // THROUGH. sample rate = 100% (not used anyway for THROUGH traces)
    TraceDecisionUtil.shouldTraceRequest("LayerC", null, null, null); // not through trace
    TraceDecisionUtil.shouldTraceRequest(
        "LayerC", X_TRACE_ID_SAMPLED, null, null); // THROUGH_TRACE_COUNT +1
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(false, false, false, false, false)
                .withSampleRate(1000000)
                .build()); // NEVER. sample rate = 100% (not used anyway as no traces get through)
    TraceDecisionUtil.shouldTraceRequest("LayerD", null, null, null); // not through trace
    TraceDecisionUtil.shouldTraceRequest(
        "LayerD", X_TRACE_ID_SAMPLED, null, null); // through flag off

    data = TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.THROUGH_TRACE_COUNT);

    assertEquals(3, data);
  }

  @Test
  public void testTriggerTraceCount() throws Exception {
    when(settingsFetcherMock.getSettings())
        .thenReturn(SettingsStub.builder().withFlags(true, false, true, true, false).build());

    int data;
    TraceDecisionUtil.consumeMetricsData(
        TraceDecisionUtil.MetricType.TRIGGERED_TRACE_COUNT); // clear it
    TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.TRACE_COUNT); // clear it
    TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.THROUGHPUT); // clear it

    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION)
                .build());
    XtraceOptions badSignatureOptions =
        new XtraceOptions(
            Collections.emptyMap(),
            Collections.emptyList(),
            XtraceOptions.AuthenticationStatus.failure("bad-signature"));
    XtraceOptions goodSignatureWithTriggerTraceOptions =
        new XtraceOptions(
            Collections.singletonMap(XtraceOption.TRIGGER_TRACE, true),
            Collections.emptyList(),
            XtraceOptions.AuthenticationStatus.OK);

    // not a trigger trace
    TraceDecisionUtil.shouldTraceRequest(
        "LayerA", null, null, null); // tracing, sample rate at 100%
    // trigger trace but no signature
    TraceDecisionUtil.shouldTraceRequest(
        "LayerA", null, TRIGGER_TRACE_OPTIONS, null); // tracing, trigger trace requested
    // bad signature
    TraceDecisionUtil.shouldTraceRequest(
        "LayerA", null, badSignatureOptions, null); // not tracing, bad signature
    // good signature with trigger trace
    TraceDecisionUtil.shouldTraceRequest(
        "LayerA",
        null,
        goodSignatureWithTriggerTraceOptions,
        null); // tracing, trigger trace requested

    // check THROUGHPUT
    data = TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.THROUGHPUT);
    // check the Layer tag
    assertEquals(4, data);

    // check TRACE_COUNT
    data = TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.TRACE_COUNT);
    // check the Layer tag
    assertEquals(3, data);

    // check TRIGGERED_TRACE_COUNT, only 2 of them are traced and flagged as trigger trace
    data = TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.TRIGGERED_TRACE_COUNT);
    assertEquals(2, data);
  }

  @Test
  public void testLastSampleRate() throws Exception {
    when(settingsFetcherMock.getSettings())
        .thenReturn(SettingsStub.builder().withFlags(true, false, true, true, false).build());

    // Add local URL rate, should override the SRv1 rate if pattern matches
    TraceConfigs testingUrlSampleRateConfigs =
        buildUrlConfigs(url -> url.endsWith("html"), TracingMode.NEVER, 0);
    SettingsManager.initialize(
        settingsFetcherMock,
        SamplingConfiguration.builder()
            .internalTransactionSettings(testingUrlSampleRateConfigs)
            .build());
    TraceDecisionUtil.consumeLastTraceConfigs(); // clear it

    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSampleRate(1000000)
                .withSettingsType(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE)
                .build()); // ALWAYS sample rate = 100%
    TraceDecisionUtil.shouldTraceRequest("LayerA", null, null, null); // 100%, ALWAYS => record 100%
    TraceDecisionUtil.shouldTraceRequest(
        "LayerA",
        null,
        null,
        Collections.singletonList("something.html")); // URL overrides, NEVER => do not record
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSampleRate(0)
                .withSettingsType(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE)
                .build()); // ALWAYS. sample rate = 0%
    TraceDecisionUtil.shouldTraceRequest("LayerB", null, null, null); // 0%, ALWAYS => record 0%
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(false, false, true, false, false)
                .withSampleRate(1000000)
                .withSettingsType(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE)
                .build()); // THROUGH. sample rate = 100% (not used anyway for THROUGH traces)
    TraceDecisionUtil.shouldTraceRequest(
        "LayerC", X_TRACE_ID_SAMPLED, null, null); // 100%, THROUGH => do not record
    TraceDecisionUtil.shouldTraceRequest(
        "LayerC", null, null, null); // 100%, THROUGH => do not record
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSampleRate(0)
                .withSettingsType(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE)
                .build()); // ALWAYS. sample rate = 0%

    Map<String, TraceConfig> data = TraceDecisionUtil.consumeLastTraceConfigs();

    assertEquals(2, data.size());
    assertEquals(1000000, data.get("LayerA").getSampleRate());
    assertEquals(SampleRateSource.OBOE, data.get("LayerA").getSampleRateSource());
    assertEquals(0, data.get("LayerB").getSampleRate());
    assertEquals(SampleRateSource.OBOE, data.get("LayerB").getSampleRateSource());
    assertFalse(data.containsKey("LayerC"));

    // case 2: add local settings sampleRate= 500000 (50%), now it should override all the oboe
    // settings
    SettingsManager.initialize(
        settingsFetcherMock,
        SamplingConfiguration.builder()
            .internalTransactionSettings(testingUrlSampleRateConfigs)
            .sampleRate(500000)
            .tracingMode(TracingMode.ALWAYS)
            .build());

    TraceDecisionUtil.shouldTraceRequest(
        "LayerA", null, null, null); // 50% (local settings), ALWAYS => record 50%
    TraceDecisionUtil.shouldTraceRequest(
        "LayerA",
        null,
        null,
        Collections.singletonList("something.html")); // URL overrides, NEVER => do not record
    TraceDecisionUtil.shouldTraceRequest(
        "LayerB", null, null, null); // 50% (local settings), ALWAYS => record 50%
    TraceDecisionUtil.shouldTraceRequest(
        "LayerC", X_TRACE_ID_SAMPLED, null, null); // 50% (local settings), ALWAYS => record 50%

    data = TraceDecisionUtil.consumeLastTraceConfigs();

    assertEquals(3, data.size());
    assertEquals(500000, data.get("LayerA").getSampleRate());
    assertEquals(SampleRateSource.FILE, data.get("LayerA").getSampleRateSource());
    assertEquals(500000, data.get("LayerB").getSampleRate());
    assertEquals(SampleRateSource.FILE, data.get("LayerB").getSampleRateSource());
    assertEquals(500000, data.get("LayerC").getSampleRate());
    assertEquals(SampleRateSource.FILE, data.get("LayerC").getSampleRateSource());
  }

  @Test
  public void testGetRemoteSampleRate() {
    Settings settings;
    TraceConfig remoteSampleRate;

    // test remote settings with no args
    settings =
        SettingsStub.builder()
            .withFlags(TracingMode.ALWAYS)
            .withSampleRate(0)
            .withSettingsArgs(Collections.emptyMap())
            .build();
    when(settingsFetcherMock.getSettings()).thenReturn(settings);
    remoteSampleRate = TraceDecisionUtil.getRemoteTraceConfig();
    assertEquals(
        0.0, remoteSampleRate.getBucketCapacity(TokenBucketType.REGULAR)); // should default to 0
    assertEquals(
        0.0, remoteSampleRate.getBucketRate(TokenBucketType.REGULAR)); // should default to 0

    // test remote settings that give empty value for "BucketCapacity" and "BucketRate"
    Map<SettingsArg<?>, Object> args = new HashMap<SettingsArg<?>, Object>();
    args.put(SettingsArg.BUCKET_CAPACITY, null);
    args.put(SettingsArg.BUCKET_RATE, null);
    settings =
        SettingsStub.builder()
            .withFlags(TracingMode.ALWAYS)
            .withSampleRate(0)
            .withSettingsArgs(args)
            .build();
    when(settingsFetcherMock.getSettings()).thenReturn(settings);
    remoteSampleRate = TraceDecisionUtil.getRemoteTraceConfig();
    assertEquals(
        0.0, remoteSampleRate.getBucketCapacity(TokenBucketType.REGULAR)); // should default to 0
    assertEquals(
        0.0, remoteSampleRate.getBucketRate(TokenBucketType.REGULAR)); // should default to 0

    // test remote settings that give negative values for "BucketCapacity" and "BucketRate"
    args = new HashMap<SettingsArg<?>, Object>();
    args.put(SettingsArg.BUCKET_CAPACITY, -1.0);
    args.put(SettingsArg.BUCKET_RATE, -2.0);
    settings =
        SettingsStub.builder()
            .withFlags(TracingMode.ALWAYS)
            .withSampleRate(0)
            .withSettingsArgs(args)
            .build();
    when(settingsFetcherMock.getSettings()).thenReturn(settings);
    remoteSampleRate = TraceDecisionUtil.getRemoteTraceConfig();
    assertEquals(
        0.0, remoteSampleRate.getBucketCapacity(TokenBucketType.REGULAR)); // should default to 0
    assertEquals(
        0.0, remoteSampleRate.getBucketRate(TokenBucketType.REGULAR)); // should default to 0

    // test remote settings that give valid values for "BucketCapacity" and "BucketRate"
    args = new HashMap<SettingsArg<?>, Object>();
    args.put(SettingsArg.BUCKET_CAPACITY, 1.0);
    args.put(SettingsArg.BUCKET_RATE, 2.0);
    settings =
        SettingsStub.builder()
            .withFlags(TracingMode.ALWAYS)
            .withSampleRate(0)
            .withSettingsArgs(args)
            .build();
    when(settingsFetcherMock.getSettings()).thenReturn(settings);
    remoteSampleRate = TraceDecisionUtil.getRemoteTraceConfig();
    assertEquals(
        1.0, remoteSampleRate.getBucketCapacity(TokenBucketType.REGULAR)); // should default to 0
    assertEquals(
        2.0, remoteSampleRate.getBucketRate(TokenBucketType.REGULAR)); // should default to 0
  }

  /**
   * Test getting local config with different trace mode and {@link
   * SamplingConfiguration#isTriggerTraceEnabled()} values
   */
  @Test
  public void testTriggerTraceConfig() {
    TraceConfig remoteConfigEnabled =
        new TraceConfig(
            TraceDecisionUtil.SAMPLE_RESOLUTION,
            SampleRateSource.OBOE_DEFAULT,
            (short)
                (Settings.OBOE_SETTINGS_FLAG_SAMPLE_START
                    | Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS
                    | Settings.OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED
                    | Settings.OBOE_SETTINGS_FLAG_OVERRIDE));
    TraceConfig remoteConfigDisabled =
        new TraceConfig(0, SampleRateSource.OBOE_DEFAULT, Settings.OBOE_SETTINGS_FLAG_OVERRIDE);
    TraceConfig localConfigDefault = new TraceConfig(null, SampleRateSource.DEFAULT, null);
    TraceConfig localConfigEnabled =
        new TraceConfig(null, SampleRateSource.FILE, TracingMode.ENABLED.toFlags());
    TraceConfig localConfigDisabled =
        new TraceConfig(0, SampleRateSource.FILE, TracingMode.DISABLED.toFlags());
    TraceConfig localConfigSampleRateConfigured =
        new TraceConfig(TraceDecisionUtil.SAMPLE_RESOLUTION, SampleRateSource.FILE, null);

    TraceConfig result;
    // Remote tracing enabled, Local tracing default, trigger trace disabled - trigger trace should
    // be disabled
    result = TraceDecisionUtil.computeTraceConfig(remoteConfigEnabled, localConfigDefault, false);
    assertFalse(result.hasSampleTriggerTraceFlag());

    // Remote tracing enabled, Local tracing enabled, trigger trace disabled - trigger trace should
    // be disabled
    result = TraceDecisionUtil.computeTraceConfig(remoteConfigEnabled, localConfigDisabled, false);
    assertFalse(result.hasSampleTriggerTraceFlag());

    // Remote tracing enabled, Local tracing sample rate configured, trigger trace disabled -
    // trigger trace should be disabled
    result =
        TraceDecisionUtil.computeTraceConfig(
            remoteConfigEnabled, localConfigSampleRateConfigured, false);
    assertFalse(result.hasSampleTriggerTraceFlag());

    // Remote tracing enabled, Local tracing default, trigger trace enabled - trigger trace should
    // be enabled
    result = TraceDecisionUtil.computeTraceConfig(remoteConfigEnabled, localConfigDefault, true);
    assertTrue(result.hasSampleTriggerTraceFlag());

    // Remote tracing enabled, Local tracing disabled, trigger trace enabled - trigger trace should
    // be disabled - local trace mode disable wins
    result = TraceDecisionUtil.computeTraceConfig(remoteConfigEnabled, localConfigDisabled, true);
    assertFalse(result.hasSampleTriggerTraceFlag());

    // Remote tracing disabled, Local tracing enabled, trigger trace enabled - trigger trace should
    // be disabled - remote trace mode disable wins
    result = TraceDecisionUtil.computeTraceConfig(remoteConfigDisabled, localConfigEnabled, true);
    assertFalse(result.hasSampleTriggerTraceFlag());
  }

  @Test
  public void testProfilingEnabledByFlagsWhenRemoteNotAvailable() {
    when(settingsFetcherMock.getSettings()).thenReturn(null);
    assertTrue(TraceDecisionUtil.isProfilingEnabledByFlags());
  }

  @Test
  public void testProfilingEnabledByFlagsWhenOverrideWithProfilingFlag() {
    when(settingsFetcherMock.getSettings())
        .thenReturn(SettingsStub.builder().withFlags(true, false, true, true, true, true).build());
    assertTrue(TraceDecisionUtil.isProfilingEnabledByFlags());
  }

  @Test
  public void testProfilingDisabledByFlagsWhenOverrideWithoutProfilingFlag() {
    when(settingsFetcherMock.getSettings())
        .thenReturn(SettingsStub.builder().withFlags(true, false, true, true, true, false).build());
    assertFalse(TraceDecisionUtil.isProfilingEnabledByFlags());
  }

  @Test
  public void testProfilingEnabledByFlagsWhenNoOverrideWithoutProfilingFlag() {
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder().withFlags(true, false, true, true, false, false).build());
    assertTrue(TraceDecisionUtil.isProfilingEnabledByFlags());
  }

  @Test
  public void testProfilingEnabledByFlagsWhenNoOverrideWithProfilingFlag() {
    when(settingsFetcherMock.getSettings())
        .thenReturn(SettingsStub.builder().withFlags(true, false, true, true, false, true).build());
    assertTrue(TraceDecisionUtil.isProfilingEnabledByFlags());
  }

  @Test
  public void testBadSignature() {
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(true, false, true, true, false)
                .withSampleRate(1000000)
                .build());
    TraceDecision traceDecision;
    traceDecision = TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, null, null, null);
    assertTrue(traceDecision.isSampled());
    assertTrue(traceDecision.isReportMetrics());

    XtraceOptions badSignatureOptions =
        new XtraceOptions(
            Collections.emptyMap(),
            Collections.emptyList(),
            XtraceOptions.AuthenticationStatus.failure("bad-signature"));
    traceDecision =
        TraceDecisionUtil.shouldTraceRequest(TEST_LAYER, null, badSignatureOptions, null);
    assertFalse(traceDecision.isSampled());
    assertTrue(traceDecision.isReportMetrics());
  }

  private static Map.Entry<String, Object> getLayerTag(String layer) {
    return getTag("Layer", layer);
  }

  private static Map.Entry<String, Object> getTag(String key, Object value) {
    return new AbstractMap.SimpleEntry<String, Object>(key, value);
  }

  private TraceConfigs buildUrlConfigs(
      ResourceMatcher matcher, TracingMode tracingMode, Integer sampleRate) {
    Map<ResourceMatcher, TraceConfig> result = new LinkedHashMap<ResourceMatcher, TraceConfig>();
    TraceConfig traceConfig = buildTraceConfig(tracingMode, sampleRate);
    result.put(matcher, traceConfig);
    return new TraceConfigs(result);
  }

  private TraceConfig buildTraceConfig(TracingMode tracingMode, Integer sampleRate) {
    // The core test case should test directly on a defined TraceConfig. The logic that builds a
    // TraceConfig is Agent specific
    return new TraceConfig(sampleRate, SampleRateSource.FILE, tracingMode.toFlags());
  }
}
