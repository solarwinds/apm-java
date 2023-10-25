package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.extensions.initialize.AutoConfiguredResourceCustomizer;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class ResourceAttributesToSystemPropertiesTest {
    @InjectMocks
    private ResourceAttributesToSystemProperties tested;

    @Mock
    private AutoConfiguredOpenTelemetrySdk sdkMock;

    @Test
    void verifyThatServiceNameIsInjectedIntoSystemProperty() {
        try (MockedStatic<AutoConfiguredResourceCustomizer> resourceCustomizerMock = mockStatic(AutoConfiguredResourceCustomizer.class)) {
            resourceCustomizerMock.when(AutoConfiguredResourceCustomizer::getResource)
                    .thenReturn(Resource.builder().put(ResourceAttributes.SERVICE_NAME, "service").build());
            tested.afterAgent(sdkMock);
            assertEquals("service", System.getProperty("service.name"));
        }
    }

    @Test
    void verifyThatOrderIsIntegerMax() {
        assertEquals(Integer.MAX_VALUE, tested.order());
    }
}