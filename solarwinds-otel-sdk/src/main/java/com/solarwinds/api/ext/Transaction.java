package com.solarwinds.api.ext;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;

/**
 * The API to set the custom transaction name for the current trace. It returns false if the current
 * trace is not valid or not sampled.
 */
class Transaction {

  /**
   * Set the transaction name of the current trace.
   *
   * @param name the custom transaction name to be set to the current trace
   * @return {@code true} if the transaction name is successfully set, or {@code false} if the
   *     transaction name is not set because the span is invalid or the not sampled or name is not
   *     valid(null or empty).
   */
  static boolean setName(String name) {
    Context context = Context.current();
    Span span = Span.fromContext(context);
    SpanContext spanContext = span.getSpanContext();

    if (!spanContext.isValid() || name == null || name.isEmpty()) {
      return false;
    }
    LocalRootSpan.fromContext(Context.current()).setAttribute("sw.transaction", name);

    return true;
  }
}
