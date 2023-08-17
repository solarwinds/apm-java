package com.appoptics.opentelemetry.extensions.initialize;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;

import java.util.function.BiFunction;

public class AutoConfiguredResourceCustomizer implements BiFunction<Resource, ConfigProperties, Resource> {
    private static Resource resource;

    @Override
    public Resource apply(Resource resource, ConfigProperties configProperties) {
        AutoConfiguredResourceCustomizer.resource = resource;
        return resource;
    }

    public static Resource getResource() {
        return resource;
    }
}
