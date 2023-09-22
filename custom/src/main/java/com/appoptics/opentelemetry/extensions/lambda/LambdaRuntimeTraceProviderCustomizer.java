package com.appoptics.opentelemetry.extensions.lambda;

import com.appoptics.opentelemetry.extensions.AppOpticsRootSpanProcessor;
import com.appoptics.opentelemetry.extensions.AppOpticsSampler;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;

import java.util.function.BiFunction;

import static com.appoptics.opentelemetry.extensions.initialize.OtelAutoConfigurationCustomizerProviderImpl.isAgentEnabled;
import static com.tracelytics.util.HostTypeDetector.isLambda;

public class LambdaRuntimeTraceProviderCustomizer implements BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder> {
    private final BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder> delegate;

    public LambdaRuntimeTraceProviderCustomizer(BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder> delegate) {
        this.delegate = delegate;
    }

    @Override
    public SdkTracerProviderBuilder apply(SdkTracerProviderBuilder sdkTracerProviderBuilder, ConfigProperties configProperties) {
        if (isAgentEnabled()) {
            if (isLambda()) {
                sdkTracerProviderBuilder
                        .setSampler(new AppOpticsSampler())
                        .addSpanProcessor(new AppOpticsRootSpanProcessor());
            } else {
                return delegate.apply(sdkTracerProviderBuilder, configProperties);
            }
        }

        return sdkTracerProviderBuilder;
    }
}
