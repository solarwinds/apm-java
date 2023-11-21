/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.core;

import io.opentelemetry.api.trace.Span;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class stores the root span of a particular trace by its trace ID.
 *
 * <p>Code logic can then perform lookup to get the root span of a particular trace anywhere by
 * providing trace ID.
 *
 * <p>Take note that this cannot be implemented using <code>io.opentelemetry.context.Context</code>
 * similar to <code>io.opentelemetry.instrumentation.api.tracer.ServerSpan</code> as the context can
 * be overwritten by OT Tracer's instrumentation
 */
public final class RootSpan {
  private static final ConcurrentHashMap<String, Span> ROOT_SPAN_LOOKUP = new ConcurrentHashMap<>();

  public static Span fromTraceId(String traceId) {
    return ROOT_SPAN_LOOKUP.get(traceId);
  }

  public static void setRootSpan(Span rootSpan) {
    ROOT_SPAN_LOOKUP.put(rootSpan.getSpanContext().getTraceId(), rootSpan);
  }

  public static Span clearRootSpan(String traceId) {
    return ROOT_SPAN_LOOKUP.remove(traceId);
  }

  private RootSpan() {}
}
