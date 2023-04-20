package com.appoptics.opentelemetry.extensions;

import io.opentelemetry.api.trace.TraceState;

public class SamplingUtil {
  private SamplingUtil(){}
  public static final String SW_TRACESTATE_KEY = "sw";
  public static final String SW_XTRACE_OPTIONS_RESP_KEY = "xtrace_options_response";
  public static final String SW_SPAN_PLACEHOLDER = "SWSpanIdPlaceHolder";
  public static final String SW_SPAN_PLACEHOLDER_SAMPLED = SW_SPAN_PLACEHOLDER + "-01";
  public static final String SW_SPAN_PLACEHOLDER_NOT_SAMPLED = SW_SPAN_PLACEHOLDER + "-00";


  public static boolean isValidSWTraceState(String swVal) {
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

  public static boolean isValidSWTraceState(TraceState traceState) {
    return isValidSWTraceState(traceState.get(SW_TRACESTATE_KEY));
  }
}
