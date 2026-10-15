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

package com.solarwinds.opentelemetry.extensions;

import com.solarwinds.joboe.sampling.XtraceOptions;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Collection;
import java.util.Collections;

public class TraceOptionsPropagator implements TextMapPropagator {
  static final String X_TRACE_OPTIONS = "X-Trace-Options";
  static final String X_TRACE_OPTIONS_SIGNATURE = "X-Trace-Options-Signature";

  @Override
  public Collection<String> fields() {
    return Collections.emptyList();
  }

  /**
   * Injects the x-trace-options and its signature into the carrier with values provided by current
   * context
   *
   * @param context trace context
   * @param carrier the input to the method being instrumented. Usually a request of some kind
   * @param setter the object that knows how to inject data into carrier using preconfigured keys
   */
  @Override
  public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
    if (carrier == null || !Span.fromContext(context).getSpanContext().isValid()) {
      return;
    }

    final String traceOptions = context.get(TriggerTraceContextKey.XTRACE_OPTIONS);
    final String traceOptionsSignature =
        context.get(TriggerTraceContextKey.XTRACE_OPTIONS_SIGNATURE);

    if (traceOptions != null) {
      setter.set(carrier, X_TRACE_OPTIONS, traceOptions);
    }

    if (traceOptionsSignature != null) {
      setter.set(carrier, X_TRACE_OPTIONS_SIGNATURE, traceOptionsSignature);
    }
  }

  /**
   * Extracts x-trace-options and its signature from carrier and populates the extracted data into
   * the current trace context.
   *
   * @param context trace context
   * @param carrier the input to the method being instrumented. Usually a request of some kind
   * @param getter the object that knows how to extract data from carrier using preconfigured keys
   * @return updated context
   */
  @Override
  public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
    final String traceOptions = getter.get(carrier, X_TRACE_OPTIONS);
    final String traceOptionsSignature = getter.get(carrier, X_TRACE_OPTIONS_SIGNATURE);

    final XtraceOptions xTraceOptions =
        XtraceOptions.getXtraceOptions(traceOptions, traceOptionsSignature);

    if (xTraceOptions != null) {
      context = context.with(TriggerTraceContextKey.KEY, xTraceOptions);
      context = context.with(TriggerTraceContextKey.XTRACE_OPTIONS, traceOptions);

      if (traceOptionsSignature != null) {
        context =
            context.with(TriggerTraceContextKey.XTRACE_OPTIONS_SIGNATURE, traceOptionsSignature);
      }
    }

    return context;
  }
}
