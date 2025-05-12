package com.solarwinds.opentelemetry.extensions.config;

import com.google.auto.service.AutoService;
import com.solarwinds.opentelemetry.extensions.SolarwindsSampler;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;

@AutoService(ComponentProvider.class)
public class SamplerComponentProvider implements ComponentProvider<Sampler> {
  @Override
  public Class<Sampler> getType() {
    return Sampler.class;
  }

  @Override
  public String getName() {
    return "swo/sampler";
  }

  @Override
  public Sampler create(
      io.opentelemetry.api.incubator.config.DeclarativeConfigProperties
          declarativeConfigProperties) {
    return new SolarwindsSampler();
  }
}
