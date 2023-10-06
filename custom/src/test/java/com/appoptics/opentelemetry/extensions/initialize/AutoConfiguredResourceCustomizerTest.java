package com.appoptics.opentelemetry.extensions.initialize;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class AutoConfiguredResourceCustomizerTest {

    @InjectMocks
    private AutoConfiguredResourceCustomizer tested;


    private final Resource resource = Resource.create(Attributes.builder()
            .put(ResourceAttributes.PROCESS_COMMAND_LINE, "-Dsw.apm.service.key=token:chubi-token")
            .put(ResourceAttributes.PROCESS_COMMAND_ARGS, Collections.singletonList("-Dsw.apm.service.key=token:chubi-token"))
            .build());

    @Test
    void verifyThatAutoConfiguredResourceIsCached() {
        tested.apply(resource, DefaultConfigProperties.create(Collections.emptyMap()));
        assertNotNull(AutoConfiguredResourceCustomizer.getResource());
    }

    @Test
    void verifyThatServiceKeyIsMaskedWhenPresentInProcessCommandLine() {
        Resource actual = tested.apply(resource, DefaultConfigProperties.create(Collections.emptyMap()));
        assertEquals("-Dsw.apm.service.key=****", actual.getAttribute(ResourceAttributes.PROCESS_COMMAND_LINE));
    }


    @Test
    void verifyThatProcessCommandLineIsNotModifiedWhenServiceKeyIsNotPresentInProcessCommandLine() {
        Resource resource = Resource.create(Attributes.builder()
                .put(ResourceAttributes.PROCESS_COMMAND_LINE, "-Duser.country=US -Duser.language=en").build());
        Resource actual = tested.apply(resource, DefaultConfigProperties.create(Collections.emptyMap()));
        assertEquals("-Duser.country=US -Duser.language=en", actual.getAttribute(ResourceAttributes.PROCESS_COMMAND_LINE));
    }

    @Test
    void verifyThatServiceKeyIsMaskedWhenPresentInProcessCommandArgs() {
        Resource actual = tested.apply(resource, DefaultConfigProperties.create(Collections.emptyMap()));
        assertEquals("-Dsw.apm.service.key=****", Objects.requireNonNull(actual.getAttribute(ResourceAttributes.PROCESS_COMMAND_ARGS)).get(0));
    }


    @Test
    void verifyThatProcessCommandLineIsNotModifiedWhenServiceKeyIsNotPresentProcessCommandArgs() {
        Resource resource = Resource.create(Attributes.builder()
                .put(ResourceAttributes.PROCESS_COMMAND_ARGS, Arrays.asList("-Duser.country=US", "-Duser.language=en")).build());
        Resource actual = tested.apply(resource, DefaultConfigProperties.create(Collections.emptyMap()));
        assertEquals(Arrays.asList("-Duser.country=US", "-Duser.language=en"), actual.getAttribute(ResourceAttributes.PROCESS_COMMAND_ARGS));
    }

}