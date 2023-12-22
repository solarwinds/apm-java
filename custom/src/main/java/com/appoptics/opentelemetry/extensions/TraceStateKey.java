package com.appoptics.opentelemetry.extensions;

import com.solarwinds.shaded.google.errorprone.annotations.Immutable;
import io.opentelemetry.context.ContextKey;

@Immutable
public class TraceStateKey {
  static final ContextKey<String> KEY = ContextKey.named("tracestate-original-value");
}
