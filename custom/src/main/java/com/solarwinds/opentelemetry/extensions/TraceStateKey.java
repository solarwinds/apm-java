package com.solarwinds.opentelemetry.extensions;

import com.solarwinds.joboe.shaded.google.errorprone.annotations.Immutable;
import io.opentelemetry.context.ContextKey;

@Immutable
public class TraceStateKey {
  static final ContextKey<String> KEY = ContextKey.named("tracestate-original-value");
}
