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

package com.solarwinds.opentelemetry.extensions;

import static com.solarwinds.opentelemetry.extensions.SamplingUtil.SW_XTRACE_OPTIONS_RESP_KEY;

import com.solarwinds.joboe.sampling.XTraceOptions;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SolarwindsContextPropagator implements TextMapPropagator {
  private static final String SWI_TRACE_STATE_KEY = "sw";
  static final String TRACE_PARENT = "traceparent";
  static final String TRACE_STATE = "tracestate";
  static final String W3C_TRACE_CONTEXT_HEADER = "sw.trace_context";
  static final String X_TRACE_OPTIONS = "X-Trace-Options";
  static final String X_TRACE_OPTIONS_SIGNATURE = "X-Trace-Options-Signature";
  private static final List<String> FIELDS =
      Collections.unmodifiableList(
          Arrays.asList(TRACE_PARENT, TRACE_STATE, W3C_TRACE_CONTEXT_HEADER));
  private static final int TRACESTATE_MAX_SIZE = 512;
  private static final int TRACESTATE_MAX_MEMBERS = 32;
  private static final int OVERSIZE_ENTRY_LENGTH = 129;
  private static final String TRACESTATE_KEY_VALUE_DELIMITER = "=";
  private static final String TRACESTATE_ENTRY_DELIMITER = ",";

  @Override
  public Collection<String> fields() {
    return FIELDS;
  }

  /**
   * Injects the both the Solarwinds x-trace ID and the updated w3c `tracestate` with our x-trace ID
   * prepended into the carrier with values provided by current context
   *
   * @param context trace context
   * @param carrier the input to the method being instrumented. Usually a request of some kind
   * @param setter the object that knows how to inject data into carrier using preconfigured keys
   */
  @Override
  public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
    final SpanContext spanContext = Span.fromContext(context).getSpanContext();
    if (carrier == null || !spanContext.isValid()) {
      return;
    }
    // update trace state too:
    // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/api.md#tracestate
    // https://www.w3.org/TR/trace-context/#mutating-the-tracestate-field
    final TraceState traceState = spanContext.getTraceState();
    final String swTraceStateValue =
        spanContext.getSpanId() + "-" + (spanContext.isSampled() ? "01" : "00");
    setter.set(carrier, TRACE_STATE, updateTraceState(traceState, swTraceStateValue));

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
   * Update tracestate with the new SW tracestate value and do some truncation if needed.
   *
   * @param traceState w3c tracestate
   * @param swTraceStateValue solarwinds tracestate
   * @return updated tracestate serialized to string
   */
  private String updateTraceState(TraceState traceState, String swTraceStateValue) {
    final StringBuilder traceStateBuilder = new StringBuilder(TRACESTATE_MAX_SIZE);
    traceStateBuilder
        .append(SWI_TRACE_STATE_KEY)
        .append(TRACESTATE_KEY_VALUE_DELIMITER)
        .append(swTraceStateValue);

    final Set<Map.Entry<String, String>> tracestateEntries = traceState.asMap().entrySet();
    int count = 1;
    // calculate current length of the tracestate
    int traceStateLength = 0;
    for (Map.Entry<String, String> entry : tracestateEntries) {
      String key = entry.getKey();
      boolean verdict = (SWI_TRACE_STATE_KEY.equals(key) || SW_XTRACE_OPTIONS_RESP_KEY.equals(key));
      if (!verdict) {
        traceStateLength += (key.length());
        traceStateLength += (TRACESTATE_KEY_VALUE_DELIMITER.length());
        traceStateLength += (entry.getValue().length());
        traceStateLength += (TRACESTATE_ENTRY_DELIMITER.length());
      }
    }

    boolean truncateLargeEntry =
        traceStateLength + traceStateBuilder.length() > TRACESTATE_MAX_SIZE;
    for (Map.Entry<String, String> entry : tracestateEntries) {
      String key = entry.getKey();
      String value = entry.getValue();
      boolean verdict = (SWI_TRACE_STATE_KEY.equals(key) || SW_XTRACE_OPTIONS_RESP_KEY.equals(key));

      final int length =
          traceStateBuilder.length()
              + TRACESTATE_ENTRY_DELIMITER.length()
              + key.length()
              + TRACESTATE_KEY_VALUE_DELIMITER.length()
              + value.length();

      if (!verdict && count < TRACESTATE_MAX_MEMBERS && length <= TRACESTATE_MAX_SIZE) {
        if (key.length() + TRACESTATE_KEY_VALUE_DELIMITER.length() + value.length()
                >= OVERSIZE_ENTRY_LENGTH
            && truncateLargeEntry) {
          truncateLargeEntry = false; // only truncate one oversize entry as SW tracestate entry is
          // smaller than OVERSIZE_ENTRY_LENGTH
        } else {
          traceStateBuilder
              .append(TRACESTATE_ENTRY_DELIMITER)
              .append(key)
              .append(TRACESTATE_KEY_VALUE_DELIMITER)
              .append(value);
          count++;
        }
      }
    }
    return traceStateBuilder.toString();
  }

  /**
   * Extracts x-trace-options and tracestate from carrier and populates the extracted data into the
   * current trace context.
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
    final XTraceOptions xTraceOptions =
        XTraceOptions.getXTraceOptions(traceOptions, traceOptionsSignature);
    if (xTraceOptions != null) {
      context = context.with(TriggerTraceContextKey.KEY, xTraceOptions);
      context = context.with(TriggerTraceContextKey.XTRACE_OPTIONS, traceOptions);
      if (traceOptionsSignature != null) {
        context =
            context.with(TriggerTraceContextKey.XTRACE_OPTIONS_SIGNATURE, traceOptionsSignature);
      }
    }

    final String traceState = getter.get(carrier, TRACE_STATE);
    if (traceState != null) {
      boolean anyMatch =
          Arrays.stream(traceState.split(",")).anyMatch(info -> !info.startsWith("sw="));
      if (anyMatch) {
        context = context.with(TraceStateKey.KEY, traceState);
      }
    }
    return context;
  }
}
