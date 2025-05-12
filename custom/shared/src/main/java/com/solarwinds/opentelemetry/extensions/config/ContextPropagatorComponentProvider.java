package com.solarwinds.opentelemetry.extensions.config;

import com.google.auto.service.AutoService;
import com.solarwinds.opentelemetry.extensions.SolarwindsContextPropagator;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;

@AutoService(ComponentProvider.class)
public class ContextPropagatorComponentProvider implements ComponentProvider<TextMapPropagator> {
  @Override
  public Class<TextMapPropagator> getType() {
    return TextMapPropagator.class;
  }

  @Override
  public String getName() {
    return "swo/contextPropagator";
  }

  @Override
  public TextMapPropagator create(DeclarativeConfigProperties declarativeConfigProperties) {
    return new SolarwindsContextPropagator();
  }
}
