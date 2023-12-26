/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation;

import com.appoptics.opentelemetry.core.Constants;
import com.solarwinds.util.BackTraceUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import java.util.LinkedHashMap;
import java.util.Map;

public class AoStatementTracer {
  private static final Map<String, String> LRUCache =
      new LinkedHashMap<String, String>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
          return size() > 128;
        }
      };

  public static void writeStackTraceSpec(Context context) {
    Span span = Span.fromContext(context);
    if (span.getSpanContext().isSampled()) {
      String backTraceString =
          LRUCache.computeIfAbsent(
              span.getSpanContext().getSpanId(),
              (ignored) -> BackTraceUtil.backTraceToString(BackTraceUtil.getBackTrace(1)));
      span.setAttribute(Constants.SW_KEY_PREFIX + "Backtrace", backTraceString);
      span.setAttribute(Constants.SW_KEY_PREFIX + "Spec", "query");
    }
  }
}
