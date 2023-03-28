/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation;

import com.appoptics.opentelemetry.core.Constants;
import com.tracelytics.util.BackTraceUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;

public class AoStatementTracer {

  public static void writeStackTraceSpec(Context context) {
    Span span = Span.fromContext(context);

    if (span.getSpanContext().isSampled()) {
      String backTraceString = BackTraceUtil.backTraceToString(BackTraceUtil.getBackTrace(1));
      span.setAttribute(Constants.SW_KEY_PREFIX + "Backtrace", backTraceString);
      span.setAttribute(Constants.SW_KEY_PREFIX + "Spec", "query");
    }
  }
}

