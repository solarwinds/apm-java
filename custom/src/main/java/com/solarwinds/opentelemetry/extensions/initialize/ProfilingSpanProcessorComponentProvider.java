package com.solarwinds.opentelemetry.extensions.initialize;

import com.google.auto.service.AutoService;
import com.solarwinds.opentelemetry.extensions.SolarwindsProfilingSpanProcessor;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;

@AutoService(ComponentProvider.class)
public class ProfilingSpanProcessorComponentProvider implements ComponentProvider<SpanProcessor> {
  @Override
  public Class<SpanProcessor> getType() {
    return SpanProcessor.class;
  }

  @Override
  public String getName() {
    return "swo/profilingSpanProcessor";
  }

  @Override
  public SpanProcessor create(DeclarativeConfigProperties declarativeConfigProperties) {
    return new SolarwindsProfilingSpanProcessor();
  }
}
