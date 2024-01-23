package com.appoptics.opentelemetry.extensions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.solarwinds.joboe.core.settings.SettingsManager;
import com.solarwinds.joboe.core.util.HostTypeDetector;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import java.util.concurrent.CountDownLatch;
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

  @Mock private SdkTracerProvider sdkTracerProviderMock;

  @Test
  void returnFalseWhenOurSamplerIsNotAttached() {
    when(autoConfiguredOpenTelemetrySdkMock.getOpenTelemetrySdk())
        .thenReturn(OpenTelemetrySdk.builder().build());

    assertFalse(tested.isUsingAppOpticsSampler(autoConfiguredOpenTelemetrySdkMock));
  }

  @Test
  void returnTrueWhenOurSamplerIsAttached() {
    when(autoConfiguredOpenTelemetrySdkMock.getOpenTelemetrySdk()).thenReturn(openTelemetrySdkMock);

    when(openTelemetrySdkMock.getSdkTracerProvider()).thenReturn(sdkTracerProviderMock);
    when(sdkTracerProviderMock.getSampler()).thenReturn(new SolarwindsSampler());

    assertTrue(tested.isUsingAppOpticsSampler(autoConfiguredOpenTelemetrySdkMock));
  }

  @Test
  void verifyThatStartUpTaskBranchIsNotEnteredWhenInLambda() {
    try (MockedStatic<HostTypeDetector> hostTypeDetectorMockedStatic =
        mockStatic(HostTypeDetector.class)) {
      hostTypeDetectorMockedStatic.when(HostTypeDetector::isLambda).thenReturn(true);
      tested.afterAgent(autoConfiguredOpenTelemetrySdkMock);
      verify(autoConfiguredOpenTelemetrySdkMock, never()).getOpenTelemetrySdk();
    }
  }

  @Test
  void verifyThatSettingsManagerIsInitializedWhenInLambda() {
    try (MockedStatic<HostTypeDetector> hostTypeDetectorMockedStatic =
            mockStatic(HostTypeDetector.class);
        MockedStatic<SettingsManager> settingsManagerMockedStatic =
            mockStatic(SettingsManager.class)) {

      settingsManagerMockedStatic
          .when(SettingsManager::initialize)
          .thenReturn(new CountDownLatch(0));
      hostTypeDetectorMockedStatic.when(HostTypeDetector::isLambda).thenReturn(true);

      tested.afterAgent(autoConfiguredOpenTelemetrySdkMock);
      settingsManagerMockedStatic.verify(SettingsManager::initialize);
    }
  }
}