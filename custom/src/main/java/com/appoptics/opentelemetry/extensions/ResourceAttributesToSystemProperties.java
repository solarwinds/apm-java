package com.appoptics.opentelemetry.extensions;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

import static com.appoptics.opentelemetry.extensions.initialize.AutoConfiguredResourceCustomizer.getResource;

@AutoService(AgentListener.class)
public class ResourceAttributesToSystemProperties implements AgentListener {

    @Override
    public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
        Resource resource = getResource();
        String serviceName = resource.getAttribute(ResourceAttributes.SERVICE_NAME);
        if (serviceName != null) {
            System.setProperty(ResourceAttributes.SERVICE_NAME.getKey(), serviceName);
        }
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }
}
