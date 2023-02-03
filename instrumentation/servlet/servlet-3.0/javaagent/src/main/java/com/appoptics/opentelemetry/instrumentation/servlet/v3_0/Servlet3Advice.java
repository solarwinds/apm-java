/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation.servlet.v3_0;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("unused")
public class Servlet3Advice {
  private static final String XTRACE_HEADER = "X-Trace"; // used for trigger trace response header only
  private static final String SW_XTRACE_OPTIONS_RESP_KEY = "xtrace_options_response";
  private static final String XTRACE_OPTIONS_RESP_HEADER = "X-Trace-Options-Response";


  @Advice.OnMethodEnter(suppress = Throwable.class)
  public static void onEnter(
      @Advice.This(typing = Assigner.Typing.DYNAMIC) Object servletOrFilter,
      @Advice.Argument(value = 0, readOnly = false) ServletRequest request,
      @Advice.Argument(value = 1, readOnly = false) ServletResponse response) {
    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      HttpServletResponse httpServletResponse = (HttpServletResponse)response;
      if (!httpServletResponse.containsHeader(XTRACE_HEADER)) {
        injectXTraceHeader(httpServletResponse);
      }
    }
  }

  public static void injectXTraceHeader(HttpServletResponse response) {
    SpanContext spanContext = Span.fromContext(Context.current()).getSpanContext();
    String flags = spanContext.isSampled() ? "01" : "00";
    String traceContext = "00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-" + flags;
    response.addHeader(XTRACE_HEADER, traceContext);

    String xTraceOptionsResp = spanContext.getTraceState().get(SW_XTRACE_OPTIONS_RESP_KEY);
    if (xTraceOptionsResp != null) {
      response.addHeader(XTRACE_OPTIONS_RESP_HEADER, recover(xTraceOptionsResp));
    }
  }

  public static String recover(String in) {
    if (in == null) {
      return in;
    }
    return in.replace("####", "=").replace("....", ",");
  }

}
