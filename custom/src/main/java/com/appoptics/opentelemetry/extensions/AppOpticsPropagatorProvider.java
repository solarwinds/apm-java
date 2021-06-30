package com.appoptics.opentelemetry.extensions;

import com.google.auto.service.AutoService;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;

@AutoService(ConfigurablePropagatorProvider.class)
public class AppOpticsPropagatorProvider implements ConfigurablePropagatorProvider {

    @Override
    public TextMapPropagator getPropagator() {
        return new AppOpticsPropagator();
    }

    @Override
    public String getName() {
        return "appoptics";
    }
}
