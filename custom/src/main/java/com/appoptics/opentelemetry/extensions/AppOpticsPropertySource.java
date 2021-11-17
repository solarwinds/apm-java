package com.appoptics.opentelemetry.extensions;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.config.ConfigPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Provide various default properties when running OT agent with AO SPI implementations
 */
@AutoService(ConfigPropertySource.class)
public class AppOpticsPropertySource implements ConfigPropertySource {
    private static final Map<String, String> PROPERTIES = new HashMap<>();

    static {
        PROPERTIES.put("otel.traces.exporter", "appoptics");
        PROPERTIES.put("otel.traces.sampler", "appoptics");
        PROPERTIES.put("otel.metrics.exporter", "none");
        PROPERTIES.put("otel.propagators", "tracecontext,baggage,appoptics");
    }

    @Override
    public Map<String, String> getProperties() {
        return PROPERTIES;
    }
}
