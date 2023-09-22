package com.appoptics.opentelemetry.extensions.lambda;

import com.appoptics.opentelemetry.extensions.AppOpticsRootSpanProcessor;
import com.appoptics.opentelemetry.extensions.AppOpticsSampler;
import com.appoptics.opentelemetry.extensions.initialize.OtelAutoConfigurationCustomizerProviderImpl;
import com.tracelytics.util.HostTypeDetector;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LambdaRuntimeTraceProviderCustomizerTest {

    @InjectMocks
    private LambdaRuntimeTraceProviderCustomizer tested;

    @Mock
    private BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder> delegateMock;

    @Mock
    private SdkTracerProviderBuilder sdkTracerProviderBuilderMock;

    @Captor
    private ArgumentCaptor<Sampler> samplerArgumentCaptor;

    @Captor
    private ArgumentCaptor<SpanProcessor> spanProcessorArgumentCaptor;

    @Test
    void verifyThatSdkTracerProviderBuilderIsNotCustomizedWhenAgentIsDisabled() {
        try (MockedStatic<OtelAutoConfigurationCustomizerProviderImpl> otelCustomizerMock = mockStatic(OtelAutoConfigurationCustomizerProviderImpl.class)) {
            otelCustomizerMock.when(OtelAutoConfigurationCustomizerProviderImpl::isAgentEnabled).thenReturn(false);

            tested.apply(sdkTracerProviderBuilderMock, DefaultConfigProperties.create(Collections.emptyMap()));
            verify(sdkTracerProviderBuilderMock, never()).setSampler(any());
        }
    }

    @Test
    void verifyThatSdkTracerProviderBuilderIsCustomizedWhenAgentIsEnabledAndInLambda() {
        try (MockedStatic<OtelAutoConfigurationCustomizerProviderImpl> otelCustomizerMock = mockStatic(OtelAutoConfigurationCustomizerProviderImpl.class);
             MockedStatic<HostTypeDetector> hostTypeDetectorMock = mockStatic(HostTypeDetector.class)) {
            otelCustomizerMock.when(OtelAutoConfigurationCustomizerProviderImpl::isAgentEnabled).thenReturn(true);
            hostTypeDetectorMock.when(HostTypeDetector::isLambda).thenReturn(true);

            when(sdkTracerProviderBuilderMock.setSampler(any())).thenReturn(sdkTracerProviderBuilderMock);
            when(sdkTracerProviderBuilderMock.addSpanProcessor(any())).thenReturn(sdkTracerProviderBuilderMock);

            tested.apply(sdkTracerProviderBuilderMock, DefaultConfigProperties.create(Collections.emptyMap()));
            verify(sdkTracerProviderBuilderMock).setSampler(samplerArgumentCaptor.capture());
            verify(sdkTracerProviderBuilderMock).addSpanProcessor(spanProcessorArgumentCaptor.capture());

            assertTrue(samplerArgumentCaptor.getValue() instanceof AppOpticsSampler);
            assertTrue(spanProcessorArgumentCaptor.getValue() instanceof AppOpticsRootSpanProcessor);
        }
    }

    @Test
    void verifyThatCustomizationIsDelegatedWhenAgentIsEnabledAndNotInLambda() {
        try (MockedStatic<OtelAutoConfigurationCustomizerProviderImpl> otelCustomizerMock = mockStatic(OtelAutoConfigurationCustomizerProviderImpl.class);) {
            otelCustomizerMock.when(OtelAutoConfigurationCustomizerProviderImpl::isAgentEnabled).thenReturn(true);

            tested.apply(sdkTracerProviderBuilderMock, DefaultConfigProperties.create(Collections.emptyMap()));
            verify(delegateMock).apply(any(), any());
        }
    }
}