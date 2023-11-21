package com.appoptics.opentelemetry.extensions;

import io.opentelemetry.context.ContextKey;
import javax.annotation.concurrent.Immutable;

@Immutable
public class TraceStateKey {
  static final ContextKey<String> KEY = ContextKey.named("tracestate-original-value");
}
