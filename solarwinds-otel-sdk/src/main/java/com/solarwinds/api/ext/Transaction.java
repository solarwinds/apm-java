/*
 * Copyright SolarWinds Worldwide, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solarwinds.api.ext;

import com.solarwinds.opentelemetry.core.CustomTransactionNameDict;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import java.util.regex.Pattern;

/**
 * The API to set the custom transaction name for the current trace. It returns false if the current
 * trace is not valid or not sampled.
 */
class Transaction {

  private static final Pattern REPLACE_PATTERN = Pattern.compile("[^-.:_\\\\/\\w? ]");

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

    CustomTransactionNameDict.set(spanContext.getTraceId(), transformTransactionName(name));
    LocalRootSpan.fromContext(Context.current())
        .setAttribute("sw.transaction", transformTransactionName(name));

    return true;
  }

  private static String transformTransactionName(String transactionName) {

    if (transactionName.length() > 255) {
      transactionName = transactionName.substring(0, 252) + "...";
    } else if (transactionName.isEmpty()) {
      transactionName = " "; // ensure that it at least has 1 character
    }

    transactionName = REPLACE_PATTERN.matcher(transactionName).replaceAll("_");
    transactionName = transactionName.toLowerCase();
    return transactionName;
  }
}
