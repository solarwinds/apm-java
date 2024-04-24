/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.solarwinds.opentelemetry.instrumentation;

import com.solarwinds.opentelemetry.core.Constants;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;

public class StatementTracer {
  public static void writeStackTraceSpec(Context context) {
    Span span = Span.fromContext(context);
    if (span.getSpanContext().isSampled()) {
      String backTraceString = BackTraceUtil.backTraceToString(BackTraceUtil.getBackTrace(1));
      span.setAttribute(Constants.SW_KEY_PREFIX + "Backtrace", backTraceString);
      span.setAttribute(Constants.SW_KEY_PREFIX + "Spec", "query");
    }
  }
}
