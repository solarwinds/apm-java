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

package com.solarwinds.opentelemetry.extensions;

import static com.solarwinds.opentelemetry.core.Constants.SW_KEY_PREFIX;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.core.profiler.Profiler;
import com.solarwinds.joboe.core.profiler.ProfilerSetting;
import com.solarwinds.joboe.sampling.Metadata;
import com.solarwinds.joboe.sampling.SamplingConfiguration;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SolarwindsProfilingSpanProcessorTest {

  private SolarwindsProfilingSpanProcessor processor;

  @Mock private ReadWriteSpan mockSpan;

  @Mock private Context mockParentContext;

  @Mock private SpanContext mockSpanContext;

  @Mock private SpanContext mockParentSpanContext;

  @Mock private SpanData mockSpanData;

  @Mock private Profiler.Profile mockProfile;

  private MockedStatic<Span> spanMock;

  private MockedStatic<Profiler> profilerMock;

  private final String traceId = "0123456789abcdef0123456789abcdef";

  private final String spanId = "0123456789abcdef";

  @BeforeEach
  void setup() {
    Metadata.setup(SamplingConfiguration.builder().build());
    processor = new SolarwindsProfilingSpanProcessor();
    spanMock = mockStatic(Span.class);

    profilerMock = mockStatic(Profiler.class);
  }

  @AfterEach
  void teardown() {
    spanMock.close();
    profilerMock.close();
  }

  @Test
  @DisplayName("onStart should not profile when span is not sampled")
  void onStartWhenSpanNotSampledShouldNotProfile() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.PROFILER, new ProfilerSetting(true, 1));
    when(mockSpan.getSpanContext()).thenReturn(mockSpanContext);
    when(mockSpanContext.isSampled()).thenReturn(false);

    processor.onStart(mockParentContext, mockSpan);
    verify(mockSpan, never()).setAttribute(anyString(), anyInt());
    profilerMock.verify(() -> Profiler.addProfiledThread(any(), any(), any()), never());
  }

  @Test
  @DisplayName("onStart should not profile when parent span is valid and local")
  void onStartWhenParentSpanValidAndLocalShouldNotProfile() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.PROFILER, new ProfilerSetting(true, 1));
    when(mockSpan.getSpanContext()).thenReturn(mockSpanContext);
    when(mockSpanContext.isSampled()).thenReturn(true);

    Span mockParentSpan = mock(Span.class);
    spanMock.when(() -> Span.fromContext(mockParentContext)).thenReturn(mockParentSpan);
    when(mockParentSpan.getSpanContext()).thenReturn(mockParentSpanContext);

    when(mockParentSpanContext.isValid()).thenReturn(true);
    when(mockParentSpanContext.isRemote()).thenReturn(false);

    processor.onStart(mockParentContext, mockSpan);
    verify(mockSpan, never()).setAttribute(anyString(), anyInt());
    profilerMock.verify(() -> Profiler.addProfiledThread(any(), any(), any()), never());
  }

  @Test
  @DisplayName("onStart should set attribute to -1 on root span when profiling is disabled")
  void onStartWhenProfilingDisabledOnRootSpanShouldSetAttributeToMinusOne()
      throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.PROFILER, new ProfilerSetting(false, 1));
    when(mockSpan.getSpanContext()).thenReturn(mockSpanContext);
    when(mockSpanContext.isSampled()).thenReturn(true);

    Span mockParentSpan = mock(Span.class);
    spanMock.when(() -> Span.fromContext(mockParentContext)).thenReturn(mockParentSpan);
    when(mockParentSpan.getSpanContext()).thenReturn(mockParentSpanContext);
    when(mockParentSpanContext.isValid()).thenReturn(false);

    processor.onStart(mockParentContext, mockSpan);
    verify(mockSpan).setAttribute(SW_KEY_PREFIX + "profile.spans", -1);
    profilerMock.verify(() -> Profiler.addProfiledThread(any(), any(), any()), never());
  }

  @Test
  @DisplayName(
      "onStart should add profiled thread when profiling is enabled on root span with valid metadata")
  void onStartWhenProfilingIsEnabledOnRootSpanWithValidMetadataShouldAddProfiledThread()
      throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.PROFILER, new ProfilerSetting(true, 1));
    when(mockSpan.getSpanContext()).thenReturn(mockSpanContext);
    when(mockSpanContext.isSampled()).thenReturn(true);

    when(mockSpanContext.getSpanId()).thenReturn(spanId);
    when(mockSpanContext.getTraceId()).thenReturn(traceId);
    when(mockSpanContext.getTraceFlags()).thenReturn(TraceFlags.getDefault());
    Span mockParentSpan = mock(Span.class);

    spanMock.when(() -> Span.fromContext(mockParentContext)).thenReturn(mockParentSpan);
    when(mockParentSpan.getSpanContext()).thenReturn(mockParentSpanContext);
    when(mockParentSpanContext.isValid()).thenReturn(false);

    processor.onStart(mockParentContext, mockSpan);
    profilerMock.verify(
        () -> Profiler.addProfiledThread(eq(Thread.currentThread()), any(), eq(traceId), any()),
        times(1));
    verify(mockSpan, never()).setAttribute(anyString(), anyInt());
  }

  @Test
  @DisplayName("onStart should not add profiled thread when metadata is invalid")
  void onStartWhenMetadataInvalidShouldNotAddProfiledThread() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.PROFILER, new ProfilerSetting(true, 1));
    when(mockSpan.getSpanContext()).thenReturn(mockSpanContext);
    when(mockSpanContext.isSampled()).thenReturn(true);

    when(mockSpanContext.getSpanId()).thenReturn("0000000000000000");
    when(mockSpanContext.getTraceId()).thenReturn("00000000000000000000000000000000");
    when(mockSpanContext.getTraceFlags()).thenReturn(TraceFlags.getDefault());

    Span mockParentSpan = mock(Span.class);
    spanMock.when(() -> Span.fromContext(mockParentContext)).thenReturn(mockParentSpan);
    when(mockParentSpan.getSpanContext()).thenReturn(mockSpanContext);

    processor.onStart(mockParentContext, mockSpan);
    profilerMock.verify(() -> Profiler.addProfiledThread(any(), any(), any()), never());
  }

  @Test
  @DisplayName("onEnding should not profile when span is not sampled")
  void onEndingWhenSpanNotSampledShouldNotProfile() {
    when(mockSpan.getSpanContext()).thenReturn(mockSpanContext);
    when(mockSpanContext.isSampled()).thenReturn(false);

    processor.onEnding(mockSpan);
    verify(mockSpan, never()).setAttribute(anyString(), anyInt());
    profilerMock.verify(() -> Profiler.stopProfile(any()), never());
  }

  @Test
  @DisplayName("onEnding should not profile when profiling is disabled")
  void onEndingWhenProfilingDisabledShouldNotProfile() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.PROFILER, new ProfilerSetting(false, 1));
    when(mockSpan.getSpanContext()).thenReturn(mockSpanContext);
    when(mockSpanContext.isSampled()).thenReturn(true);

    processor.onEnding(mockSpan);
    verify(mockSpan, never()).setAttribute(anyString(), anyInt());
    profilerMock.verify(() -> Profiler.stopProfile(any()), never());
  }

  @Test
  @DisplayName("onEnding should not call stop profile when parent span is valid and local")
  void onEndingWhenParentSpanValidAndLocalShouldNotCallStopProfile() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.PROFILER, new ProfilerSetting(true, 1));
    when(mockSpan.getSpanContext()).thenReturn(mockSpanContext);
    when(mockSpanContext.isSampled()).thenReturn(true);

    when(mockSpan.toSpanData()).thenReturn(mockSpanData);
    when(mockSpanData.getParentSpanContext()).thenReturn(mockParentSpanContext);
    when(mockParentSpanContext.isValid()).thenReturn(true);
    when(mockParentSpanContext.isRemote()).thenReturn(false);

    processor.onEnding(mockSpan);
    verify(mockSpan, never()).setAttribute(anyString(), anyInt());
    profilerMock.verify(() -> Profiler.stopProfile(any()), never());
  }

  @Test
  @DisplayName("onEnding should set attribute to 1 when profile has sample")
  void onEndingShouldSetAttributeToOneWhenProfileHasSample() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.PROFILER, new ProfilerSetting(true, 1));

    when(mockSpan.getSpanContext()).thenReturn(mockSpanContext);
    when(mockSpanContext.isSampled()).thenReturn(true);
    when(mockSpanContext.getTraceId()).thenReturn(traceId);
    when(mockSpanContext.getSpanId()).thenReturn(spanId);

    when(mockSpan.toSpanData()).thenReturn(mockSpanData);
    when(mockSpanData.getParentSpanContext()).thenReturn(mockParentSpanContext);
    when(mockParentSpanContext.isValid()).thenReturn(false);

    profilerMock.when(() -> Profiler.stopProfile(traceId)).thenReturn(mockProfile);
    when(mockProfile.isSampled()).thenReturn(true);

    processor.onEnding(mockSpan);
    verify(mockSpan).setAttribute(SW_KEY_PREFIX + "profile.spans", 1);
    profilerMock.verify(() -> Profiler.stopProfile(traceId), times(1));
  }

  @Test
  @DisplayName("onEnding should set attribute to 0 when profile has no sample")
  void onEndingShouldSetAttributeToZeroWhenProfileHasNoSample() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.PROFILER, new ProfilerSetting(true, 1));
    when(mockSpan.getSpanContext()).thenReturn(mockSpanContext);
    when(mockSpanContext.isSampled()).thenReturn(true);

    when(mockSpanContext.getTraceId()).thenReturn(traceId);
    when(mockSpanContext.getSpanId()).thenReturn(spanId);
    when(mockSpan.toSpanData()).thenReturn(mockSpanData);

    when(mockSpanData.getParentSpanContext()).thenReturn(mockParentSpanContext);
    when(mockProfile.isSampled()).thenReturn(false);
    profilerMock.when(() -> Profiler.stopProfile(traceId)).thenReturn(mockProfile);

    processor.onEnding(mockSpan);
    verify(mockSpan).setAttribute(SW_KEY_PREFIX + "profile.spans", 0);
    profilerMock.verify(() -> Profiler.stopProfile(traceId), times(1));
  }

  @Test
  @DisplayName("isStartRequired should return true")
  void isStartRequiredShouldReturnTrue() {
    assertTrue(processor.isStartRequired());
  }

  @Test
  @DisplayName("isEndRequired should return false")
  void isEndRequiredShouldReturnFalse() {
    assertFalse(processor.isEndRequired());
  }

  @Test
  @DisplayName("isOnEndingRequired should return true")
  void isOnEndingRequiredShouldReturnTrue() {
    assertTrue(processor.isOnEndingRequired());
  }
}
