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
            @Advice.Argument(value = 1, readOnly = false) ServletResponse response,
            @Advice.Local("otelCallDepth") CallDepth callDepth) {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            return;
        }

        callDepth = CallDepth.forClass(AppServerBridge.getCallDepthKey());
        callDepth.getAndIncrement();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
            @Advice.Argument(0) ServletRequest request,
            @Advice.Argument(1) ServletResponse response,
            @Advice.Thrown Throwable throwable,
            @Advice.Local("otelCallDepth") CallDepth callDepth) {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            return;
        }

        if (callDepth.decrementAndGet() != 0) {
            return;
        }
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        Context context = Context.current();
        Span span = Span.fromContext(context);
        SpanContext spanContext = span.getSpanContext();
        String traceId = spanContext.getTraceId();
        String spanId = spanContext.getSpanId();
        String flags = spanContext.isSampled() ? "01" : "00";
        httpServletResponse.addHeader(XTRACE_HEADER, "00-" + traceId + "-" + spanId + "-" + flags);
    }
}
