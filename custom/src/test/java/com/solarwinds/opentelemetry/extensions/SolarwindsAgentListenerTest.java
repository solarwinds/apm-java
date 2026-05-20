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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SolarwindsAgentListenerTest {

  private static final MethodHandle AGENT_ENABLED_SETTER;

  static {
    try {
      Field field = AutoConfigurationCustomizerProviderImpl.class.getDeclaredField("agentEnabled");
      field.setAccessible(true);
      AGENT_ENABLED_SETTER = MethodHandles.lookup().unreflectSetter(field);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @InjectMocks private SolarwindsAgentListener tested;

  @AfterEach
  void teardown() throws Throwable {
    // Reset the static agentEnabled field to true so it doesn't affect other tests
    AGENT_ENABLED_SETTER.invokeExact(true);
  }

  @Mock private AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdkMock;

  @Mock private OpenTelemetrySdk openTelemetrySdkMock;

  @Test
  void verifySdkIsShutdownWhenBranchIsNotTaken() {
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
  void verifySdkIsNotShutdownWhenBranchIsTaken() {
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
