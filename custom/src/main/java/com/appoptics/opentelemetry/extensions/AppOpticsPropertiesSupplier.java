package com.appoptics.opentelemetry.extensions;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Provide various default properties when running OT agent with AO SPI implementations
 */

public class AppOpticsPropertiesSupplier implements Supplier<Map<String, String>> {
    private static final Map<String, String> PROPERTIES = new HashMap<>();

    static {
        if (AppOpticsTracerProviderCustomizer.getAgentEnabled()) {
            PROPERTIES.put("otel.traces.exporter", "appoptics");
            PROPERTIES.put("otel.traces.sampler", "appoptics");
            PROPERTIES.put("otel.metrics.exporter", "none");
            PROPERTIES.put("otel.propagators", "tracecontext,baggage,appoptics");
        } else {
            PROPERTIES.put("otel.javaagent.enabled", "false");
        }
    }

    @Override
    public Map<String, String> get() {
        return PROPERTIES;
    }
}
