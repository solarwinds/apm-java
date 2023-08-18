package com.appoptics.opentelemetry.extensions.initialize;

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class AutoConfiguredResourceCustomizerTest {

    @InjectMocks
    private AutoConfiguredResourceCustomizer tested;

    @Mock
    private Resource resourceMock;

    @Test
    void verifyThatAutoConfiguredResourceIsCached(){
        tested.apply(resourceMock, DefaultConfigProperties.create(Collections.emptyMap()));

        assertNotNull(AutoConfiguredResourceCustomizer.getResource());
    }

}