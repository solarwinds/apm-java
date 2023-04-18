package com.appoptics.opentelemetry.extensions;


import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import javax.annotation.Nonnull;

@AutoService(ConfigurableSpanExporterProvider.class)
public class AppOpticsSpanExporterProvider implements ConfigurableSpanExporterProvider {
    @Override
    public SpanExporter createExporter(@Nonnull ConfigProperties config) {
        return new AppOpticsSpanExporter();
    }

    @Override
    public String getName() {
        return "appoptics";
    }
}
