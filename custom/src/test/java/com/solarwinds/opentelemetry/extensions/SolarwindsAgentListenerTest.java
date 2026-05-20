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

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solarwinds.opentelemetry.extensions.config.provider.AutoConfigurationCustomizerProviderImpl;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SolarwindsAgentListenerTest {
  @InjectMocks private SolarwindsAgentListener tested;

  @Mock private AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdkMock;

  @Mock private OpenTelemetrySdk openTelemetrySdkMock;

  @Test
  void verifySDKIsShutdownWhenBranchIsNotTaken() {
    try (MockedStatic<AutoConfigurationCustomizerProviderImpl>
        autoConfigurationCustomizerProviderMockedStatic =
            mockStatic(AutoConfigurationCustomizerProviderImpl.class)) {

      autoConfigurationCustomizerProviderMockedStatic
          .when(AutoConfigurationCustomizerProviderImpl::isAgentEnabled)
          .thenReturn(false);

      when(autoConfiguredOpenTelemetrySdkMock.getOpenTelemetrySdk())
          .thenReturn(openTelemetrySdkMock);

      tested.afterAgent(autoConfiguredOpenTelemetrySdkMock);
      verify(openTelemetrySdkMock).shutdown();
    }
  }

  @Test
  void verifySDKIsNotShutdownWhenBranchIsTaken() {
    try (MockedStatic<AutoConfigurationCustomizerProviderImpl>
        autoConfigurationCustomizerProviderMockedStatic =
            mockStatic(AutoConfigurationCustomizerProviderImpl.class)) {

      autoConfigurationCustomizerProviderMockedStatic
          .when(AutoConfigurationCustomizerProviderImpl::isAgentEnabled)
          .thenReturn(true);

      tested.afterAgent(autoConfiguredOpenTelemetrySdkMock);
      verify(openTelemetrySdkMock, never()).shutdown();
    }
  }
}
