package com.appoptics.opentelemetry.extensions;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppOpticsAgentListenerTest {
  @InjectMocks
  private AppOpticsAgentListener agentListener;

  @Mock
  private AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdkMock;

  @Mock
  private OpenTelemetrySdk openTelemetrySdkMock;

  @Mock
  private SdkTracerProvider sdkTracerProviderMock;

  @Test
  void returnFalseWhenOurSamplerIsNotAttached() {
    when(autoConfiguredOpenTelemetrySdkMock.getOpenTelemetrySdk())
            .thenReturn(OpenTelemetrySdk.builder().build());

    assertFalse(agentListener.isUsingAppOpticsSampler(autoConfiguredOpenTelemetrySdkMock));
  }

  @Test
  void returnTrueWhenOurSamplerIsAttached() {
    when(autoConfiguredOpenTelemetrySdkMock.getOpenTelemetrySdk())
            .thenReturn(openTelemetrySdkMock);

    when(openTelemetrySdkMock.getSdkTracerProvider()).thenReturn(sdkTracerProviderMock);
    when(sdkTracerProviderMock.getSampler()).thenReturn(new AppOpticsSampler());

    assertTrue(agentListener.isUsingAppOpticsSampler(autoConfiguredOpenTelemetrySdkMock));
  }
}