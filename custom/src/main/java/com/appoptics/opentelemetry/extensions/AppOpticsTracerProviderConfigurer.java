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
    public static class UnsupportedJdkVersion extends Exception {
        UnsupportedJdkVersion(String version) {
            super("Unsupported Java runtime version: " + version);
        }
    }
    private static final Logger logger = LoggerFactory.getLogger(AppOpticsTracerProviderConfigurer.class);
    private static boolean agentEnabled = true;

    static {
        try {
            if (!isJDKSupported()) {
                throw new UnsupportedJdkVersion(System.getProperty("java.version"));
            }
            Initializer.initialize();
        } catch (InvalidConfigException | UnsupportedJdkVersion e) {
            logger.warn("Agent is disabled: ", e);
            agentEnabled = false;
        }
    }
    public AppOpticsTracerProviderConfigurer() {
    }

    public static boolean isJDKSupported() {
        String version = System.getProperty("java.version");
        // TODO
        return true;
    }

    public static boolean getAgentEnabled() {
        return agentEnabled;
    }

    @Override
    public void configure(SdkTracerProviderBuilder tracerProvider, ConfigProperties config) {
        if (!agentEnabled) {
            return;
        }

        tracerProvider.addSpanProcessor(new AppOpticsRootSpanProcessor());
        tracerProvider.addSpanProcessor(new AppOpticsProfilingSpanProcessor());
        tracerProvider.addSpanProcessor(new AppOpticsInboundMetricsSpanProcessor());
    }
}
