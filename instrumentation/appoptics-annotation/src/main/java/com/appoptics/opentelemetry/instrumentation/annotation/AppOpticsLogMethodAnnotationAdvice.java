/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation.annotation;

import com.appoptics.api.ext.LogMethod;
import com.appoptics.api.ext.ProfileMethod;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

import static com.appoptics.opentelemetry.instrumentation.annotation.AppOpticsAnnotationTracer.tracer;

public class AppOpticsLogMethodAnnotationAdvice {

    @Advice.OnMethodEnter()
    public static void onEnter(
            @Advice.Origin Method method,
            @Advice.Local("otelContext") Context context,
            @Advice.Local("otelScope") Scope scope,
            @Advice.Local("annotation") LogMethod annotation) {
        annotation = method.getAnnotation(LogMethod.class);
        SpanKind kind = SpanKind.INTERNAL;
        Context current = Context.current();

        context = tracer().startSpan(current, annotation, method, kind);
        scope = context.makeCurrent();
    }

    //@Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    @Advice.OnMethodExit
    public static void stopSpan(
            @Advice.Local("otelContext") Context context,
            @Advice.Local("otelScope") Scope scope,
            @Advice.Local("annotation") LogMethod annotation,
            @Advice.Return Object returnValue,
            @Advice.Thrown Throwable throwable) {
        if (scope == null) {
            return;
        }

        tracer().endMethod(context, annotation.storeReturn(), returnValue);

        if (annotation.reportExceptions() && throwable != null) {
            tracer().endExceptionally(context, throwable);
        } else {
            tracer().end(context);
        }
        scope.close();
    }
}
