package com.appoptics.opentelemetry.extensions;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.spi.config.PropertySource;

import java.util.HashMap;
import java.util.Map;

//public class AppOpticsPropertySource {
//    //1.1.0 does not have this
//}
@AutoService(PropertySource.class)
public class AppOpticsPropertySource implements PropertySource {
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
