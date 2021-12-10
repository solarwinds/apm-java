/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation.annotation;

import com.appoptics.api.ext.ProfileMethod;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

import static com.appoptics.opentelemetry.instrumentation.annotation.AppOpticsAnnotationTracer.tracer;

public class AppOpticsProfileMethodAnnotationAdvice {
    @Advice.OnMethodEnter()
    public static void onEnter(
            @Advice.Origin Method method,
            @Advice.Local("otelContext") Context context,
            @Advice.Local("otelScope") Scope scope,
            @Advice.Local("annotation") ProfileMethod annotation) {
        annotation = method.getAnnotation(ProfileMethod.class);
        SpanKind kind = SpanKind.INTERNAL;
        Context current = Context.current();

        context = tracer().startSpan(current, annotation, method, kind);
        scope = context.makeCurrent();
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void stopSpan(
            @Advice.Local("otelContext") Context context,
            @Advice.Local("otelScope") Scope scope,
            @Advice.Local("annotation") ProfileMethod annotation,
            @Advice.Return Object returnValue) {
        if (scope == null) {
            return;
        }

        tracer().endMethod(context, annotation.storeReturn(), returnValue);

        tracer().end(context);

        scope.close();
    }
}
