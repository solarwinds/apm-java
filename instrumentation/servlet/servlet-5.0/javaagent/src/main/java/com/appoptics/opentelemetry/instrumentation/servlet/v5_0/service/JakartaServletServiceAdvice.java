/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation.servlet.v5_0.service;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.servlet.AppServerBridge;
import io.opentelemetry.javaagent.instrumentation.api.CallDepth;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

@SuppressWarnings("unused")
public class JakartaServletServiceAdvice {
    private static final String XTRACE_HEADER = "X-Trace"; // used for trigger trace response header only

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
            @Advice.This(typing = Assigner.Typing.DYNAMIC) Object servletOrFilter,
            @Advice.Argument(value = 0, readOnly = false) ServletRequest request,
            @Advice.Argument(value = 1, readOnly = false) ServletResponse response) {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            return;
        }
        CallDepth callDepth = CallDepth.forClass(AppServerBridge.getCallDepthKey());
        if (callDepth.get() > 1) {
            return;
        }
        injectXTraceHeader(response);
    }

    public static void injectXTraceHeader(ServletResponse response) {
        SpanContext spanContext = Span.fromContext(Context.current()).getSpanContext();
        String flags = spanContext.isSampled() ? "01" : "00";
        String traceContext = "00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-" + flags;
        ((HttpServletResponse) response).addHeader(XTRACE_HEADER, traceContext);
    }
}
