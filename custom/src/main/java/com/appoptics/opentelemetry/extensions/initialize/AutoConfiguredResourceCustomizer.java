package com.appoptics.opentelemetry.extensions.initialize;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class AutoConfiguredResourceCustomizer implements BiFunction<Resource, ConfigProperties, Resource> {
    private static Resource resource;

    @Override
    public Resource apply(Resource resource, ConfigProperties configProperties) {
        AutoConfiguredResourceCustomizer.resource = resource;
        String resourceAttribute = resource.getAttribute(ResourceAttributes.PROCESS_COMMAND_LINE);
        List<String> processArgs = resource.getAttribute(ResourceAttributes.PROCESS_COMMAND_ARGS);
        ResourceBuilder resourceBuilder = resource.toBuilder();

        if (resourceAttribute != null) {
            resourceBuilder.put(ResourceAttributes.PROCESS_COMMAND_LINE, resourceAttribute.replaceAll("(sw.apm.service.key=)\\S+", "$1****"));
        }

        if (processArgs != null){
            List<String> args = processArgs.stream().map(
                    arg -> arg.replaceAll("(sw.apm.service.key=)\\S+", "$1****")
            ).collect(Collectors.toList());
            resourceBuilder.put(ResourceAttributes.PROCESS_COMMAND_ARGS, args);
        }

        return resourceBuilder.build();
    }

    public static Resource getResource() {
        return resource;
    }
}
