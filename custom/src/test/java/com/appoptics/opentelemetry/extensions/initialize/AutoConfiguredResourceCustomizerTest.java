package com.appoptics.opentelemetry.extensions.initialize;

import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.util.ServiceKeyUtils;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

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


    @Test
    void verifyThatServiceNameIsSetWhenNonExist() {
        try (MockedStatic<AppOpticsConfigurationLoader> configLoaderMock = mockStatic(AppOpticsConfigurationLoader.class)) {
            configLoaderMock.when(() -> AppOpticsConfigurationLoader.mergeEnvWithSysProperties(any(), any()))
                    .thenReturn(new HashMap<String, String>() {{
                        put(ConfigProperty.AGENT_SERVICE_KEY.getEnvironmentVariableKey(), "token:my-name-is");
                    }});

            Resource actual = tested.apply(resource, DefaultConfigProperties.create(Collections.emptyMap()));
            assertEquals("my-name-is", actual.getAttribute(ResourceAttributes.SERVICE_NAME));
        }
    }


    @Test
    void verifyThatServiceNameIsNotModifiedWhenItExist() {
        Resource resource = Resource.create(Attributes.builder()
                .put(ResourceAttributes.SERVICE_NAME, "default-name").build());
        Resource actual = tested.apply(resource, DefaultConfigProperties.create(Collections.emptyMap()));
        assertEquals("default-name", actual.getAttribute(ResourceAttributes.SERVICE_NAME));
    }

    @Test
    void verifyThatServiceNameIsModifiedWhenUnknown() {
        try (MockedStatic<AppOpticsConfigurationLoader> configLoaderMock = mockStatic(AppOpticsConfigurationLoader.class)) {
            configLoaderMock.when(() -> AppOpticsConfigurationLoader.mergeEnvWithSysProperties(any(), any()))
                    .thenReturn(new HashMap<String, String>() {{
                        put(ConfigProperty.AGENT_SERVICE_KEY.getEnvironmentVariableKey(), "token:my-name-is");
                    }});

            Resource resource = Resource.create(Attributes.builder()
                    .put(ResourceAttributes.SERVICE_NAME, "unknown_default-name").build());
            Resource updateResource = tested.apply(resource, DefaultConfigProperties.create(Collections.emptyMap()));
            assertEquals("my-name-is", updateResource.getAttribute(ResourceAttributes.SERVICE_NAME));
        }
    }


    @Test
    void verifyThatServiceKeyIsUpdatedWithOtelServiceName() throws InvalidConfigException {
        ConfigManager.setConfig(ConfigProperty.AGENT_SERVICE_KEY, "token:chubi-zero");
        Resource resource = Resource.create(Attributes.builder()
                .put(ResourceAttributes.SERVICE_NAME, "chubi-one").build());

        String serviceKeyBefore = (String) ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY);
        tested.apply(resource, DefaultConfigProperties.create(Collections.emptyMap()));
        String serviceKeyAfter = (String) ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY);

        assertNotEquals(serviceKeyBefore, serviceKeyAfter);
        assertEquals("chubi-zero", ServiceKeyUtils.getServiceName(serviceKeyBefore));
        assertEquals("chubi-one", ServiceKeyUtils.getServiceName(serviceKeyAfter));
        assertEquals("token:chubi-one", serviceKeyAfter);
    }

}