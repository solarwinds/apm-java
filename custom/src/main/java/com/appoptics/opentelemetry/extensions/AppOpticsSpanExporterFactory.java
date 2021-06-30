package com.appoptics.opentelemetry.extensions;


import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.spi.exporter.SpanExporterFactory;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;

/**
 * This is largely the same as {@link AppOpticsSpanExporterProvider}, however,
 * such factory has classloading issue with the OT auto agent
 *
 * Using this is more of the "legacy" way, and is specified via -Dotel.javaagent.experimental.exporter.jar
 * with the full jar path. This DOES work with the auto agent, but looks like it might get removed soon?
 */
@AutoService(SpanExporterFactory.class)
public class AppOpticsSpanExporterFactory implements SpanExporterFactory {
    private static final String APPOPTICS_SERVICE_KEY = "otel.appoptics.service.key";

    @Override
    public SpanExporter fromConfig(Properties properties) {
        final String serviceKey = properties.getProperty(APPOPTICS_SERVICE_KEY);
        return AppOpticsSpanExporter.newBuilder(serviceKey).build();
    }

    @Override
    public Set<String> getNames() {
        return Collections.singleton("appoptics");
    }
}
