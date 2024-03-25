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
