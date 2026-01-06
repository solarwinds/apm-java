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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.config.JavaRuntimeVersionChecker;
import com.solarwinds.opentelemetry.extensions.config.provider.AutoConfigurationCustomizerProviderImpl;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SolarwindsTracerProviderCustomizerTest {

  @InjectMocks private SolarwindsTracerProviderCustomizer tested;

  @Mock private SdkTracerProviderBuilder tracerProviderBuilderMock;

  @Mock private ConfigProperties configPropertiesMock;

  private MockedStatic<AutoConfigurationCustomizerProviderImpl>
      autoConfigurationCustomizerProviderImplMockedStatic;

  @BeforeEach
  void setUp() {
    autoConfigurationCustomizerProviderImplMockedStatic =
        mockStatic(AutoConfigurationCustomizerProviderImpl.class);
  }

  @AfterEach
  void tearDown() {
    autoConfigurationCustomizerProviderImplMockedStatic.close();
  }

  @Test
  void verifyThatProfilingSpanProcessorIsAddedWhenAgentEnabledAndJdkVersionSupported() {
    try (MockedStatic<JavaRuntimeVersionChecker> javaRuntimeVersionCheckerMockedStatic =
        mockStatic(JavaRuntimeVersionChecker.class)) {

      autoConfigurationCustomizerProviderImplMockedStatic
          .when(AutoConfigurationCustomizerProviderImpl::isAgentEnabled)
          .thenReturn(true);

      javaRuntimeVersionCheckerMockedStatic
          .when(JavaRuntimeVersionChecker::isJdkVersionSupported)
          .thenReturn(true);

      when(tracerProviderBuilderMock.addSpanProcessor(any(SpanProcessor.class)))
          .thenReturn(tracerProviderBuilderMock);
      when(tracerProviderBuilderMock.setSampler(any(Sampler.class)))
          .thenReturn(tracerProviderBuilderMock);

      tested.apply(tracerProviderBuilderMock, configPropertiesMock);

      // Should add SolarwindsProfilingSpanProcessor and InboundMeasurementMetricsGenerator
      verify(tracerProviderBuilderMock, times(2)).addSpanProcessor(any(SpanProcessor.class));
      verify(tracerProviderBuilderMock).setSampler(any(Sampler.class));
    }
  }

  @Test
  void verifyThatProfilingSpanProcessorIsNotAddedWhenJdkVersionNotSupported() {
    try (MockedStatic<JavaRuntimeVersionChecker> javaRuntimeVersionCheckerMockedStatic =
        mockStatic(JavaRuntimeVersionChecker.class)) {

      autoConfigurationCustomizerProviderImplMockedStatic
          .when(AutoConfigurationCustomizerProviderImpl::isAgentEnabled)
          .thenReturn(true);

      javaRuntimeVersionCheckerMockedStatic
          .when(JavaRuntimeVersionChecker::isJdkVersionSupported)
          .thenReturn(false);

      when(tracerProviderBuilderMock.addSpanProcessor(any(SpanProcessor.class)))
          .thenReturn(tracerProviderBuilderMock);
      when(tracerProviderBuilderMock.setSampler(any(Sampler.class)))
          .thenReturn(tracerProviderBuilderMock);

      tested.apply(tracerProviderBuilderMock, configPropertiesMock);

      // Should only add InboundMeasurementMetricsGenerator, not SolarwindsProfilingSpanProcessor
      verify(tracerProviderBuilderMock, times(1)).addSpanProcessor(any(SpanProcessor.class));
      verify(tracerProviderBuilderMock).setSampler(any(Sampler.class));
    }
  }

  @Test
  void verifyThatNoProcessorsAddedWhenAgentDisabled() {
    autoConfigurationCustomizerProviderImplMockedStatic
        .when(AutoConfigurationCustomizerProviderImpl::isAgentEnabled)
        .thenReturn(false);

    tested.apply(tracerProviderBuilderMock, configPropertiesMock);

    verify(tracerProviderBuilderMock, never()).addSpanProcessor(any(SpanProcessor.class));
    verify(tracerProviderBuilderMock, never()).setSampler(any(Sampler.class));
  }
}
