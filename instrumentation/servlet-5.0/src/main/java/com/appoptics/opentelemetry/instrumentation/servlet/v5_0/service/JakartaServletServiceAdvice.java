/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation.servlet.v5_0.service;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

@SuppressWarnings("unused")
public class JakartaServletServiceAdvice {
  private static final String XTRACE_HEADER =
      "X-Trace"; // used for trigger trace response header only
  private static final String SW_XTRACE_OPTIONS_RESP_KEY =
      "xtrace_options_response"; // use for trigger trace only
  private static final String XTRACE_OPTIONS_RESP_HEADER = "X-Trace-Options-Response";

  @Advice.OnMethodEnter(suppress = Throwable.class)
  public static void onEnter(
      @Advice.This(typing = Assigner.Typing.DYNAMIC) Object servletOrFilter,
      @Advice.Argument(value = 0, readOnly = false) ServletRequest request,
      @Advice.Argument(value = 1, readOnly = false) ServletResponse response) {
    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      HttpServletResponse httpServletResponse = (HttpServletResponse) response;
      if (!httpServletResponse.containsHeader(XTRACE_HEADER)) {
        injectXtraceHeader(httpServletResponse);
      }
    }
  }

  public static void injectXtraceHeader(HttpServletResponse response) {
    SpanContext spanContext = Span.fromContext(Context.current()).getSpanContext();
    String flags = spanContext.isSampled() ? "01" : "00";
    String traceContext =
        "00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-" + flags;
    response.addHeader(XTRACE_HEADER, traceContext);

    String xtraceOptionsResp = spanContext.getTraceState().get(SW_XTRACE_OPTIONS_RESP_KEY);
    if (xtraceOptionsResp != null) {
      response.addHeader(XTRACE_OPTIONS_RESP_HEADER, recover(xtraceOptionsResp));
    }
  }

  public static String recover(String in) {
    if (in == null) {
      return in;
    }
    return in.replace("####", "=").replace("....", ",");
  }
}
