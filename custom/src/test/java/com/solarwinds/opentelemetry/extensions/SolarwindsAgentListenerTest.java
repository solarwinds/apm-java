package com.solarwinds.opentelemetry.extensions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
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
}
