package com.solarwinds.opentelemetry.extensions.lambda;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.solarwinds.joboe.core.util.HostTypeDetector;
import com.solarwinds.opentelemetry.extensions.SolarwindsSampler;
import com.solarwinds.opentelemetry.extensions.initialize.AutoConfigurationCustomizerProviderImpl;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Collections;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuntimeTraceProviderCustomizerTest {

  @InjectMocks private RuntimeTraceProviderCustomizer tested;

  @Mock
  private BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder>
      delegateMock;

  @Mock private SdkTracerProviderBuilder sdkTracerProviderBuilderMock;

  @Captor private ArgumentCaptor<Sampler> samplerArgumentCaptor;

  @Test
  void verifyThatSdkTracerProviderBuilderIsNotCustomizedWhenAgentIsDisabled() {
    try (MockedStatic<AutoConfigurationCustomizerProviderImpl> otelCustomizerMock =
        mockStatic(AutoConfigurationCustomizerProviderImpl.class)) {
      otelCustomizerMock
          .when(AutoConfigurationCustomizerProviderImpl::isAgentEnabled)
          .thenReturn(false);

      tested.apply(
          sdkTracerProviderBuilderMock, DefaultConfigProperties.create(Collections.emptyMap()));
      verify(sdkTracerProviderBuilderMock, never()).setSampler(any());
    }
  }

  @Test
  void verifyThatSdkTracerProviderBuilderIsCustomizedWhenAgentIsEnabledAndInLambda() {
    try (MockedStatic<AutoConfigurationCustomizerProviderImpl> otelCustomizerMock =
            mockStatic(AutoConfigurationCustomizerProviderImpl.class);
        MockedStatic<HostTypeDetector> hostTypeDetectorMock = mockStatic(HostTypeDetector.class)) {
      otelCustomizerMock
          .when(AutoConfigurationCustomizerProviderImpl::isAgentEnabled)
          .thenReturn(true);
      hostTypeDetectorMock.when(HostTypeDetector::isLambda).thenReturn(true);

      when(sdkTracerProviderBuilderMock.setSampler(any())).thenReturn(sdkTracerProviderBuilderMock);
      when(sdkTracerProviderBuilderMock.addSpanProcessor(any()))
          .thenReturn(sdkTracerProviderBuilderMock);

      tested.apply(
          sdkTracerProviderBuilderMock, DefaultConfigProperties.create(Collections.emptyMap()));
      verify(sdkTracerProviderBuilderMock).setSampler(samplerArgumentCaptor.capture());
      verify(sdkTracerProviderBuilderMock, atMost(2)).addSpanProcessor(any());

      assertTrue(samplerArgumentCaptor.getValue() instanceof SolarwindsSampler);
    }
  }

  @Test
  void verifyThatCustomizationIsDelegatedWhenAgentIsEnabledAndNotInLambda() {
    try (MockedStatic<AutoConfigurationCustomizerProviderImpl> otelCustomizerMock =
        mockStatic(AutoConfigurationCustomizerProviderImpl.class); ) {
      otelCustomizerMock
          .when(AutoConfigurationCustomizerProviderImpl::isAgentEnabled)
          .thenReturn(true);

      tested.apply(
          sdkTracerProviderBuilderMock, DefaultConfigProperties.create(Collections.emptyMap()));
      verify(delegateMock).apply(any(), any());
    }
  }
}
