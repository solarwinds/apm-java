package com.solarwinds.opentelemetry.extensions;

import com.solarwinds.joboe.core.TraceDecision;
import com.solarwinds.joboe.core.TraceDecisionUtil;
import com.solarwinds.joboe.core.XTraceOption;
import com.solarwinds.joboe.core.XTraceOptions;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.TraceState;

public class SamplingUtil {
  private SamplingUtil() {}

  public static final String SW_TRACESTATE_KEY = "sw";
  public static final String SW_XTRACE_OPTIONS_RESP_KEY = "xtrace_options_response";
  public static final String SW_SPAN_PLACEHOLDER = "SWSpanIdPlaceHolder";
  public static final String SW_SPAN_PLACEHOLDER_SAMPLED = SW_SPAN_PLACEHOLDER + "-01";
  public static final String SW_SPAN_PLACEHOLDER_NOT_SAMPLED = SW_SPAN_PLACEHOLDER + "-00";

  public static boolean isValidSwTraceState(TraceState traceState) {
    return isValidSwTraceState(traceState.get(SW_TRACESTATE_KEY));
  }

  public static boolean isValidSwTraceState(String swVal) {
    if (swVal == null || !swVal.contains("-")) {
      return false;
    }
    final String[] swTraceState = swVal.split("-");
    if (swTraceState.length != 2) {
      return false;
    }

    // 16 is the hex length of the Otel trace id
    return (swTraceState[0].equals(SW_SPAN_PLACEHOLDER) || swTraceState[0].length() == 16)
        && (swTraceState[1].equals("00") || swTraceState[1].equals("01"));
  }

  public static boolean isSwSpanPlaceHolder(TraceState traceState) {
    String swTracestate = traceState.get(SW_TRACESTATE_KEY);
    return swTracestate != null && SW_SPAN_PLACEHOLDER.equals(swTracestate.split("-")[0]);
  }

  public static void addXtraceOptionsToAttribute(
      TraceDecision traceDecision,
      XTraceOptions xtraceOptions,
      AttributesBuilder attributesBuilder) {
    if (xtraceOptions != null) {
      xtraceOptions
          .getCustomKvs()
          .forEach(
              ((stringXtraceOption, s) -> attributesBuilder.put(stringXtraceOption.getKey(), s)));

      if (traceDecision.getRequestType()
              == TraceDecisionUtil.RequestType.AUTHENTICATED_TRIGGER_TRACE
          || traceDecision.getRequestType()
              == TraceDecisionUtil.RequestType.UNAUTHENTICATED_TRIGGER_TRACE) {
        attributesBuilder.put("TriggeredTrace", true);
      }

      String swKeys = xtraceOptions.getOptionValue(XTraceOption.SW_KEYS);
      if (swKeys != null) {
        attributesBuilder.put("SWKeys", swKeys);
      }
    }
  }
}
