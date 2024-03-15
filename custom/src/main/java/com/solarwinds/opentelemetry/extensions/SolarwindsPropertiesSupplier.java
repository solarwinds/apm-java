package com.solarwinds.opentelemetry.extensions;

import static com.solarwinds.opentelemetry.extensions.initialize.AutoConfigurationCustomizerProviderImpl.isAgentEnabled;
import static com.solarwinds.opentelemetry.extensions.initialize.config.ConfigConstants.COMPONENT_NAME;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Provide various default properties when running OT agent with AO SPI implementations */
public class SolarwindsPropertiesSupplier implements Supplier<Map<String, String>> {
  private static final Map<String, String> PROPERTIES = new HashMap<>();

  static {
    if (isAgentEnabled()) {
      PROPERTIES.put("otel.traces.exporter", COMPONENT_NAME);
      PROPERTIES.put("otel.metrics.exporter", "none");
      PROPERTIES.put("otel.propagators", String.format("tracecontext,baggage,%s", COMPONENT_NAME));
    } else {
      PROPERTIES.put("otel.sdk.disabled", "true");
    }
  }

  @Override
  public Map<String, String> get() {
    return PROPERTIES;
  }
}
