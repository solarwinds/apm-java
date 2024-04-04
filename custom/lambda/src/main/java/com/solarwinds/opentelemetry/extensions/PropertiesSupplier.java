package com.solarwinds.opentelemetry.extensions;

import static com.solarwinds.opentelemetry.extensions.SharedNames.COMPONENT_NAME;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PropertiesSupplier implements Supplier<Map<String, String>> {

  private final Map<String, String> defaultProperties = new HashMap<>();

  public PropertiesSupplier() {
    defaultProperties.put(
        "otel.propagators", String.format("tracecontext,baggage,%s,xray", COMPONENT_NAME));
    defaultProperties.put("otel.instrumentation.runtime-telemetry.enabled", "false");
    defaultProperties.put("otel.exporter.otlp.protocol", "grpc");
  }

  @Override
  public Map<String, String> get() {
    return defaultProperties;
  }
}
