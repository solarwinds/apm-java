package com.solarwinds.opentelemetry.extensions.config.provider;

import com.google.auto.service.AutoService;
import com.solarwinds.opentelemetry.extensions.SolarwindsSampler;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class SamplerComponentProvider implements ComponentProvider<Sampler> {

  public static final String COMPONENT_NAME = "swo/sampler";

  @Override
  public Class<Sampler> getType() {
    return Sampler.class;
  }

  @Override
  public String getName() {
    return COMPONENT_NAME;
  }

  @Override
  public Sampler create(
      io.opentelemetry.api.incubator.config.DeclarativeConfigProperties
          declarativeConfigProperties) {
    return new SolarwindsSampler();
  }
}
