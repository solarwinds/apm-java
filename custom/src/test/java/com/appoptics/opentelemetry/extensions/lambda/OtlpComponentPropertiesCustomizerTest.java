package com.appoptics.opentelemetry.extensions.lambda;

import com.tracelytics.util.HostTypeDetector;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class OtlpComponentPropertiesCustomizerTest {

    private final OtlpComponentPropertiesCustomizer tested = new OtlpComponentPropertiesCustomizer();
    @Test
    void verifyThatEmptyMapIsReturnedWhenNotInLambda() {
        Map<String, String> configUpdate = tested.apply(DefaultConfigProperties.create(Collections.emptyMap()));
        assertTrue(configUpdate.isEmpty());
    }

    @Test
    void verifyThatUpdateMapIsReturnedWhenInLambda() {
        try(MockedStatic<HostTypeDetector> hostTypeDetectorMockedStatic = mockStatic(HostTypeDetector.class)){
            hostTypeDetectorMockedStatic.when(HostTypeDetector::isLambda).thenReturn(true);
            Map<String, String> configUpdate = tested.apply(DefaultConfigProperties.create(Collections.emptyMap()));
            assertFalse(configUpdate.isEmpty());

            assertEquals("otlp", configUpdate.get("otel.traces.exporter"));
            assertEquals("otlp", configUpdate.get("otel.metrics.exporter"));
        }
    }

    @Test
    void verifyThatConcatenatedConfigsIsReturnedWhenInLambda() {
        try(MockedStatic<HostTypeDetector> hostTypeDetectorMockedStatic = mockStatic(HostTypeDetector.class)){
            hostTypeDetectorMockedStatic.when(HostTypeDetector::isLambda).thenReturn(true);
            Map<String, String> configUpdate = tested.apply(DefaultConfigProperties.create(new HashMap<>() {{
                put("otel.traces.exporter", "logging");
                put("otel.metrics.exporter", "logging");
            }}));
            assertFalse(configUpdate.isEmpty());

            assertEquals("otlp,logging", configUpdate.get("otel.traces.exporter"));
            assertEquals("otlp,logging", configUpdate.get("otel.metrics.exporter"));
        }
    }
}