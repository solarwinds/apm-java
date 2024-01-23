package com.appoptics.opentelemetry.extensions;

import static com.appoptics.opentelemetry.extensions.initialize.config.ConfigConstants.COMPONENT_NAME;

import com.google.auto.service.AutoService;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;

@AutoService(ConfigurablePropagatorProvider.class)
public class SolarwindsContextPropagatorProvider implements ConfigurablePropagatorProvider {

  @Override
  public TextMapPropagator getPropagator(ConfigProperties config) {
    return new SolarwindsContextPropagator();
  }

  @Override
  public String getName() {
    return COMPONENT_NAME;
  }
}
