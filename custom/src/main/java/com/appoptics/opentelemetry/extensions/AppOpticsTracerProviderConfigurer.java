package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.extensions.initialize.Initializer;
import com.google.auto.service.AutoService;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.util.ServiceKeyUtils;
import io.opentelemetry.sdk.autoconfigure.spi.SdkTracerProviderConfigurer;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@AutoService(SdkTracerProviderConfigurer.class)
public class AppOpticsTracerProviderConfigurer implements SdkTracerProviderConfigurer {
    private static final String APPOPTICS_SERVICE_KEY = "otel.appoptics.service.key";
    //private Logger log = LoggerFactory.getLogger(AgentInstaller.class);
    private Logger logger = LoggerFactory.getLogger(getClass());
    public AppOpticsTracerProviderConfigurer() {
        String serviceKey = System.getProperty(APPOPTICS_SERVICE_KEY);
        try {
            Initializer.initialize(serviceKey);
            logger.info("Successfully initialized AppOptics OpenTelemetry extensions with service key " + ServiceKeyUtils.maskServiceKey(serviceKey));
        } catch (InvalidConfigException e) {
            logger.warn("Failed to initialize AppOptics OpenTelemetry extensions due to config error: " + e.getMessage(), e);
        }

    }

    @Override
    public void configure(SdkTracerProviderBuilder tracerProvider) {
        tracerProvider.addSpanProcessor(new AppOpticsRootSpanProcessor());
        tracerProvider.addSpanProcessor(new AppOpticsProfilingSpanProcessor());
        tracerProvider.addSpanProcessor(new AppOpticsInboundMetricsSpanProcessor());
    }
}
