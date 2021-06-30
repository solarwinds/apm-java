/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.extensions;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class encapsulates the context key for storing the current {@link SpanKind#SERVER} span in
 * the {@link Context}.
 */
public final class RootSpan {
  // Keeps track of the root span for the current trace.
  private static final ContextKey<Span> KEY =
      ContextKey.named("appoptics-root-span-key");

  @Nullable
  public static Span fromContextOrNull(Context context) {
    return context.get(KEY);
  }

  static Context with(Context context, Span rootSpan) {
    return context.with(KEY, rootSpan);
  }

  private RootSpan() {}
}
