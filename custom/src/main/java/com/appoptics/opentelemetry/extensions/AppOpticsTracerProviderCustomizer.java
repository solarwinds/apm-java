package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.extensions.initialize.Initializer;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.util.JavaRuntimeVersionChecker;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;


public class AppOpticsTracerProviderCustomizer implements BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder> {
    public static class UnsupportedJdkVersion extends Exception {
        UnsupportedJdkVersion(String version) {
            super("Unsupported Java runtime version: " + version);
        }
    }
    private static final Logger logger = LoggerFactory.getLogger(AppOpticsTracerProviderCustomizer.class);
    private static boolean agentEnabled = true;

    static {
        try {
            if (!JavaRuntimeVersionChecker.isJdkVersionSupported()) {
                throw new UnsupportedJdkVersion(System.getProperty("java.version"));
            }
            Initializer.initialize();
        } catch (InvalidConfigException | UnsupportedJdkVersion e) {
            logger.warn("Agent is disabled: ", e);
            agentEnabled = false;
        }
    }

    public AppOpticsTracerProviderCustomizer() {
    }

    public static boolean getAgentEnabled() {
        return agentEnabled;
    }

    @Override
    public SdkTracerProviderBuilder apply(SdkTracerProviderBuilder tracerProvider, ConfigProperties config) {
        if (agentEnabled) {
            tracerProvider.addSpanProcessor(new AppOpticsRootSpanProcessor());
            tracerProvider.addSpanProcessor(new AppOpticsProfilingSpanProcessor());
            tracerProvider.addSpanProcessor(new AppOpticsInboundMetricsSpanProcessor());
        }
        return tracerProvider;
    }
}
