/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation;

import com.tracelytics.util.BackTraceUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.tracer.BaseTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AoStatementTracer extends BaseTracer {
  private static final Logger log = LoggerFactory.getLogger(AoStatementTracer.class);

  public static void writeStackTrace(Context context) {
    Span span = Span.fromContext(context);

    if (span.getSpanContext().isSampled()) {
      String backTraceString = BackTraceUtil.backTraceToString(BackTraceUtil.getBackTrace(1));
      //span.setAttribute(Constants.AO_KEY_PREFIX + "Backtrace", backTraceString);
      span.setAttribute("ao." + "Backtrace", backTraceString);
    }
  }

  @Override
  protected String getInstrumentationName() {
    return "com.appoptics.opentelemetry.jdbc";
  }

}

