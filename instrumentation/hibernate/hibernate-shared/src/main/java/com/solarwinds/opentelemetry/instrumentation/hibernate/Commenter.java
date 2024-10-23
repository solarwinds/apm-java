/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
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

package com.solarwinds.opentelemetry.instrumentation.hibernate;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;

public final class Commenter {
  public static String generateComment(Context context) {
    Span span = Span.fromContext(context);
    SpanContext spanContext = span.getSpanContext();
    if (!(spanContext.isValid() && spanContext.isSampled())) {
      return null;
    }

    String traceContext =
        "00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-" + "01";

    String tag = String.format("/*traceparent='%s'*/", traceContext);
    span.setAttribute("QueryTag", tag);
    return tag;
  }

  private Commenter() {}
}
