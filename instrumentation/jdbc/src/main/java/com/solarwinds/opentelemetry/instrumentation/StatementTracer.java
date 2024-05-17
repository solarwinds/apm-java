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
