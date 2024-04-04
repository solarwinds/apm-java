package com.solarwinds.opentelemetry.extensions;

import io.opentelemetry.context.ContextKey;

public class TraceStateKey {
  static final ContextKey<String> KEY = ContextKey.named("tracestate-original-value");
}
