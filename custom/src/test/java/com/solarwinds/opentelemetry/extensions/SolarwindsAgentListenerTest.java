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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.opentelemetry.extensions.config.SdkResourceServiceName;
import com.solarwinds.opentelemetry.extensions.config.provider.AutoConfigurationCustomizerProviderImpl;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.internal.ExtendedOpenTelemetrySdk;
import io.opentelemetry.sdk.internal.SdkConfigProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.ServiceAttributes;
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
    ConfigManager.reset();
    SdkResourceServiceName.reset();
  }

  @Mock private AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdkMock;

  @Mock private OpenTelemetrySdk openTelemetrySdkMock;

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

  @Test
  void resolveServiceNameReadsDeclarativeSdkResource() {
    try (OpenTelemetrySdk sdk = declarativeSdk("SDK Service")) {
      assertEquals("SDK Service", SolarwindsAgentListener.resolveServiceName(sdk));
    }
  }

  @Test
  void resolveServiceNameReadsSystemConfigSdkResource() {
    try (OpenTelemetrySdk sdk = sdk(resource("System Service"))) {
      assertEquals("System Service", SolarwindsAgentListener.resolveServiceName(sdk));
    }
  }

  @Test
  void resolveServiceNameReturnsDefaultResourceServiceName() {
    try (OpenTelemetrySdk sdk = declarativeSdk(null)) {
      assertEquals("unknown_service:java", SolarwindsAgentListener.resolveServiceName(sdk));
    }
  }

  @Test
  void resolveServiceNameReturnsNullWithoutSdk() {
    assertNull(SolarwindsAgentListener.resolveServiceName(null));
  }

  @Test
  void profilingServiceKeyAttachesNormalizedServiceNameWithoutChangingConfig()
      throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_SERVICE_KEY, "token");

    assertEquals("token:sdk-service", SolarwindsAgentListener.profilingServiceKey("SDK Service"));
    assertEquals("token", ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY));
  }

  @Test
  void profilingServiceKeyReplacesServiceKeyName() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_SERVICE_KEY, "token:key-service");

    assertEquals("token:sdk-service", SolarwindsAgentListener.profilingServiceKey("sdk-service"));
  }

  @Test
  void profilingServiceKeyUsesDefaultServiceNameWhenNoneIsAvailable()
      throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_SERVICE_KEY, "token");

    assertEquals("token:unknown_service:java", SolarwindsAgentListener.profilingServiceKey(null));
  }

  private static OpenTelemetrySdk declarativeSdk(String serviceName) {
    return ExtendedOpenTelemetrySdk.create(
        sdk(resource(serviceName)), SdkConfigProvider.create(DeclarativeConfigProperties.empty()));
  }

  private static OpenTelemetrySdk sdk(Resource resource) {
    return OpenTelemetrySdk.builder()
        .setTracerProvider(SdkTracerProvider.builder().setResource(resource).build())
        .setMeterProvider(SdkMeterProvider.builder().setResource(resource).build())
        .setLoggerProvider(SdkLoggerProvider.builder().setResource(resource).build())
        .build();
  }

  private static Resource resource(String serviceName) {
    return serviceName == null
        ? Resource.getDefault()
        : Resource.getDefault()
            .merge(Resource.create(Attributes.of(ServiceAttributes.SERVICE_NAME, serviceName)));
  }
}
