package com.appoptics.opentelemetry.extensions;


import com.appoptics.opentelemetry.extensions.initialize.config.ConfigConstants;
import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;

@AutoService(ConfigurableSpanExporterProvider.class)
public class AppOpticsSpanExporterProvider implements ConfigurableSpanExporterProvider {
    @Override
    public SpanExporter createExporter(ConfigProperties config) {
        final String serviceKey = config.getString(ConfigConstants.SYS_PROPERTY_SERVICE_KEY);
        return AppOpticsSpanExporter.newBuilder(serviceKey).build();
    }

    @Override
    public String getName() {
        return "appoptics";
    }
}
