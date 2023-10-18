package com.appoptics.opentelemetry.extensions.lambda;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.tracelytics.util.HostTypeDetector.isLambda;

public class OtlpComponentPropertiesCustomizer implements Function<ConfigProperties, Map<String, String>> {
    @Override
    public Map<String, String> apply(ConfigProperties configProperties) {
        if (isLambda()) {
            String defaultTrace = configProperties.getString("otel.traces.exporter");
            String defaultMetric = configProperties.getString("otel.metrics.exporter");

            return new HashMap<String, String>() {{
                put("otel.traces.exporter", concatenate(defaultTrace));
                put("otel.metrics.exporter", concatenate(defaultMetric));
            }};
        }
        return Collections.emptyMap();
    }

    private String concatenate(String value){
        if (value == null){
            return "otlp";
        }
        return String.format("otlp,%s", value);
    }
}
