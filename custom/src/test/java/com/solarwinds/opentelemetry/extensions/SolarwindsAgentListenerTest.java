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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SolarwindsAgentListenerTest {
  @InjectMocks private SolarwindsAgentListener tested;

  @Mock private AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdkMock;

  @Mock private OpenTelemetrySdk openTelemetrySdkMock;

  @Mock private SdkTracerProvider sdkTracerProviderMock;

  @Mock private Sampler samplerMock;

  @Test
  void returnFalseWhenOurSamplerIsNotAttached() {
    when(autoConfiguredOpenTelemetrySdkMock.getOpenTelemetrySdk())
        .thenReturn(OpenTelemetrySdk.builder().build());

    assertFalse(tested.isUsingSolarwindsSampler(autoConfiguredOpenTelemetrySdkMock));
  }

  @Test
  void returnTrueWhenOurSamplerIsAttached() {
    when(autoConfiguredOpenTelemetrySdkMock.getOpenTelemetrySdk()).thenReturn(openTelemetrySdkMock);

    when(openTelemetrySdkMock.getSdkTracerProvider()).thenReturn(sdkTracerProviderMock);
    when(sdkTracerProviderMock.getSampler()).thenReturn(new SolarwindsSampler());

    assertTrue(tested.isUsingSolarwindsSampler(autoConfiguredOpenTelemetrySdkMock));
  }

  @Test
  void verifySDKIsShutdownWhenBranchIsNotTaken() {
    when(autoConfiguredOpenTelemetrySdkMock.getOpenTelemetrySdk()).thenReturn(openTelemetrySdkMock);
    when(openTelemetrySdkMock.getSdkTracerProvider()).thenReturn(sdkTracerProviderMock);
    when(sdkTracerProviderMock.getSampler()).thenReturn(samplerMock);

    tested.afterAgent(autoConfiguredOpenTelemetrySdkMock);
    verify(openTelemetrySdkMock).shutdown();
  }
}
