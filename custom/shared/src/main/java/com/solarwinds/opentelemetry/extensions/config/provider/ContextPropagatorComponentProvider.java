package com.solarwinds.opentelemetry.extensions.config.provider;

import com.google.auto.service.AutoService;
import com.solarwinds.opentelemetry.extensions.SolarwindsContextPropagator;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class ContextPropagatorComponentProvider implements ComponentProvider<TextMapPropagator> {

  public static final String COMPONENT_NAME = "swo/contextPropagator";

  @Override
  public Class<TextMapPropagator> getType() {
    return TextMapPropagator.class;
  }

  @Override
  public String getName() {
    return COMPONENT_NAME;
  }

  @Override
  public TextMapPropagator create(DeclarativeConfigProperties declarativeConfigProperties) {
    return new SolarwindsContextPropagator();
  }
}
