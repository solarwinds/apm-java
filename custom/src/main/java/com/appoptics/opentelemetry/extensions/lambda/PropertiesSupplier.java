package com.appoptics.opentelemetry.extensions.lambda;

import static com.solarwinds.joboe.core.util.HostTypeDetector.isLambda;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

public class PropertiesSupplier implements Supplier<Map<String, String>> {
  private final Supplier<Map<String, String>> delegate;

  public PropertiesSupplier(Supplier<Map<String, String>> delegate) {
    this.delegate = delegate;
  }

  @Override
  public Map<String, String> get() {
    if (isLambda()) {
      return Collections.singletonMap("otel.instrumentation.runtime-telemetry.enabled", "false");
    }
    return delegate.get();
  }
}
