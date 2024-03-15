package com.solarwinds.opentelemetry.extensions.lambda;

import static com.solarwinds.joboe.core.util.HostTypeDetector.isLambda;
import static com.solarwinds.opentelemetry.extensions.initialize.config.ConfigConstants.COMPONENT_NAME;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PropertiesSupplier implements Supplier<Map<String, String>> {
  private final Supplier<Map<String, String>> delegate;

  private final Map<String, String> defaultProperties = new HashMap<>();

  public PropertiesSupplier(Supplier<Map<String, String>> delegate) {
    this.delegate = delegate;
    defaultProperties.put(
        "otel.propagators", String.format("tracecontext,baggage,%s,xray", COMPONENT_NAME));
    defaultProperties.put("otel.instrumentation.runtime-telemetry.enabled", "false");
    defaultProperties.put("otel.exporter.otlp.protocol", "grpc");
  }

  @Override
  public Map<String, String> get() {
    if (isLambda()) {
      return defaultProperties;
    }
    return delegate.get();
  }
}
