package com.appoptics.opentelemetry.extensions;

import static com.appoptics.opentelemetry.extensions.initialize.config.ConfigConstants.COMPONENT_NAME;

import com.google.auto.service.AutoService;
import com.solarwinds.shaded.javax.annotation.Nonnull;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;

@AutoService(ConfigurableSpanExporterProvider.class)
public class AppOpticsSpanExporterProvider implements ConfigurableSpanExporterProvider {
  @Override
  public SpanExporter createExporter(@Nonnull ConfigProperties config) {
    return new AppOpticsSpanExporter();
  }

  @Override
  public String getName() {
    return COMPONENT_NAME;
  }
}
