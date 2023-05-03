package com.appoptics.opentelemetry.extensions;

import com.google.auto.service.AutoService;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;

import static com.appoptics.opentelemetry.extensions.initialize.config.ConfigConstants.COMPONENT_NAME;

@AutoService(ConfigurablePropagatorProvider.class)
public class AppOpticsContextPropagatorProvider implements ConfigurablePropagatorProvider {

    @Override
    public TextMapPropagator getPropagator(ConfigProperties config) {
        return new AppOpticsContextPropagator();
    }

    @Override
    public String getName() {
        return COMPONENT_NAME;
    }
}
