package com.solarwinds.opentelemetry.extensions;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TraceProviderCustomizerTest {

  @InjectMocks private TraceProviderCustomizer tested;

  @Mock private SdkTracerProviderBuilder sdkTracerProviderBuilderMock;

  @Captor private ArgumentCaptor<Sampler> samplerArgumentCaptor;
  @Captor private ArgumentCaptor<SpanProcessor> processorArgumentCaptor;

  @Test
  void verifyThatSdkTracerProviderBuilderIsCustomized() {
    when(sdkTracerProviderBuilderMock.setSampler(any())).thenReturn(sdkTracerProviderBuilderMock);
    when(sdkTracerProviderBuilderMock.addSpanProcessor(any()))
        .thenReturn(sdkTracerProviderBuilderMock);

    tested.apply(
        sdkTracerProviderBuilderMock, DefaultConfigProperties.create(Collections.emptyMap()));
    verify(sdkTracerProviderBuilderMock).setSampler(samplerArgumentCaptor.capture());
    verify(sdkTracerProviderBuilderMock).addSpanProcessor(processorArgumentCaptor.capture());

    assertTrue(samplerArgumentCaptor.getValue() instanceof SolarwindsSampler);
    assertTrue(processorArgumentCaptor.getValue() instanceof InboundMeasurementMetricsGenerator);
  }
}
