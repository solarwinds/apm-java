package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.extensions.initialize.Initializer;
import com.google.auto.service.AutoService;
import com.tracelytics.joboe.config.InvalidConfigException;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.SdkTracerProviderConfigurer;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@AutoService(SdkTracerProviderConfigurer.class)
public class AppOpticsTracerProviderConfigurer implements SdkTracerProviderConfigurer {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public AppOpticsTracerProviderConfigurer() {
        try {
            Initializer.initialize();
        } catch (InvalidConfigException e) {
            logger.warn(e.getMessage());
        }
    }

    @Override
    public void configure(SdkTracerProviderBuilder tracerProvider, ConfigProperties config) {
        tracerProvider.addSpanProcessor(new AppOpticsRootSpanProcessor());
        tracerProvider.addSpanProcessor(new AppOpticsProfilingSpanProcessor());
        tracerProvider.addSpanProcessor(new AppOpticsInboundMetricsSpanProcessor());
    }
}
