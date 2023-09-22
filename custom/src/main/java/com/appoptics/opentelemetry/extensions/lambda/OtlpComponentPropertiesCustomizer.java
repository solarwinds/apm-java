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
            return new HashMap<String, String>() {{
                put("otel.traces.exporter", "otlp");
                put("otel.metrics.exporter", "otlp");
            }};
        }
        return Collections.emptyMap();
    }
}
