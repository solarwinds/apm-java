package com.appoptics.opentelemetry.extensions;


import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * Newer way to provider span exporter - does not work with auto agent yet due to classloading.
 * See current discussion https://github.com/open-telemetry/opentelemetry-java/discussions/3024
 */
@AutoService(ConfigurableSpanExporterProvider.class)
public class AppOpticsSpanExporterProvider implements ConfigurableSpanExporterProvider {
    private static final String APPOPTICS_SERVICE_KEY = "otel.appoptics.service.key";

    @Override
    public SpanExporter createExporter(ConfigProperties config) {
        final String serviceKey = config.getString(APPOPTICS_SERVICE_KEY);
        return AppOpticsSpanExporter.newBuilder(serviceKey).build();
    }

    @Override
    public String getName() {
        return "appoptics";
    }
}
