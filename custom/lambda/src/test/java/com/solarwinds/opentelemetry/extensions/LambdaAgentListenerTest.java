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
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.sampling.SettingsManager;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LambdaAgentListenerTest {
  @InjectMocks private LambdaAgentListener tested;

  @Mock private AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdkMock;

  @Mock private OpenTelemetrySdk openTelemetrySdkMock;

  @Mock private SdkTracerProvider sdkTracerProviderMock;

  @Mock private Sampler samplerMock;

  @Test
  void verifySettingsManagerIsInitializedWhenConditionsAreMet() {
    try (MockedStatic<SettingsManager> settingsManagerMock = mockStatic(SettingsManager.class);
        MockedStatic<DefaultAutoConfigurationCustomizerProvider>
            defaultAutoConfigurationCustomizerProviderMock =
                mockStatic(DefaultAutoConfigurationCustomizerProvider.class)) {

      defaultAutoConfigurationCustomizerProviderMock
          .when(DefaultAutoConfigurationCustomizerProvider::isAgentEnabled)
          .thenReturn(true);
      when(autoConfiguredOpenTelemetrySdkMock.getOpenTelemetrySdk())
          .thenReturn(openTelemetrySdkMock);
      when(openTelemetrySdkMock.getSdkTracerProvider()).thenReturn(sdkTracerProviderMock);

      when(sdkTracerProviderMock.getSampler()).thenReturn(new SolarwindsSampler());

      tested.afterAgent(autoConfiguredOpenTelemetrySdkMock);
      settingsManagerMock.verify(() -> SettingsManager.initialize(any(), any()));
    }
  }

  @Test
  void verifySettingsManagerIsNotInitialized() {
    try (MockedStatic<SettingsManager> settingsManagerMock = mockStatic(SettingsManager.class);
        MockedStatic<DefaultAutoConfigurationCustomizerProvider>
            defaultAutoConfigurationCustomizerProviderMock =
                mockStatic(DefaultAutoConfigurationCustomizerProvider.class)) {

      defaultAutoConfigurationCustomizerProviderMock
          .when(DefaultAutoConfigurationCustomizerProvider::isAgentEnabled)
          .thenReturn(true);
      when(autoConfiguredOpenTelemetrySdkMock.getOpenTelemetrySdk())
          .thenReturn(openTelemetrySdkMock);
      when(openTelemetrySdkMock.getSdkTracerProvider()).thenReturn(sdkTracerProviderMock);

      when(sdkTracerProviderMock.getSampler()).thenReturn(samplerMock);

      tested.afterAgent(autoConfiguredOpenTelemetrySdkMock);
      settingsManagerMock.verify(() -> SettingsManager.initialize(any(), any()), never());
    }
  }
}
