package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.extensions.initialize.OtelAutoConfigurationCustomizerProviderImpl;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
  void verifyThatSetupTaskIsNotRunWhenOurSamplerIsNotAttached() {
    try(MockedStatic<OtelAutoConfigurationCustomizerProviderImpl> otelCustomizerProviderMock =
            mockStatic(OtelAutoConfigurationCustomizerProviderImpl.class)){

      otelCustomizerProviderMock.when(OtelAutoConfigurationCustomizerProviderImpl::isAgentEnabled).thenReturn(true);
      when(autoConfiguredOpenTelemetrySdkMock.getOpenTelemetrySdk())
          .thenReturn(OpenTelemetrySdk.builder().build());

      agentListener.afterAgent(autoConfiguredOpenTelemetrySdkMock);
      verify(autoConfiguredOpenTelemetrySdkMock, never()).getResource();
    }

  }

  @Test
  void verifyThatSetupTaskIsRunWhenOurSamplerIsAttached() {
    try(MockedStatic<OtelAutoConfigurationCustomizerProviderImpl> otelCustomizerProviderMock =
            mockStatic(OtelAutoConfigurationCustomizerProviderImpl.class)){

      otelCustomizerProviderMock.when(OtelAutoConfigurationCustomizerProviderImpl::isAgentEnabled).thenReturn(true);
      when(autoConfiguredOpenTelemetrySdkMock.getOpenTelemetrySdk())
          .thenReturn(openTelemetrySdkMock);

      when(openTelemetrySdkMock.getSdkTracerProvider()).thenReturn(sdkTracerProviderMock);
      when(sdkTracerProviderMock.getSampler()).thenReturn(new AppOpticsSampler());

      agentListener.afterAgent(autoConfiguredOpenTelemetrySdkMock);
      verify(autoConfiguredOpenTelemetrySdkMock, atMostOnce()).getResource();
    }
  }
}