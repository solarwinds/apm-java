package com.solarwinds.opentelemetry.extensions.provider;

import com.google.auto.service.AutoService;
import com.solarwinds.opentelemetry.extensions.SolarwindsSpanExporter;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;

@AutoService(ComponentProvider.class)
public class SpanExportComponentProvider implements ComponentProvider<SpanExporter> {
  @Override
  public Class<SpanExporter> getType() {
    return SpanExporter.class;
  }

  @Override
  public String getName() {
    return "swo/spanExporter";
  }

  @Override
  public SpanExporter create(DeclarativeConfigProperties declarativeConfigProperties) {
    return new SolarwindsSpanExporter();
  }
}
