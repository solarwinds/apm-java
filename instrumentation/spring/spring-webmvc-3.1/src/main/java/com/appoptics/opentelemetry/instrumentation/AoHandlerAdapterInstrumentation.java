/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.tracer.ServerSpan;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import javax.servlet.http.HttpServletRequest;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Experimental instrumentation to set `TransactionName` KV to OT Trace root span for Spring MVC
 */
public class AoHandlerAdapterInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("org.springframework.web.servlet.HandlerAdapter");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("org.springframework.web.servlet.HandlerAdapter"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(nameStartsWith("handle"))
            .and(takesArgument(0, named("javax.servlet.http.HttpServletRequest")))
            .and(takesArguments(3)),
        AoHandlerAdapterInstrumentation.class.getName() + "$ControllerAdvice");
  }

  public static class ControllerAdvice {
    @Advice.OnMethodEnter
    public static void setTransactionNameToServerSpan(
            @Advice.Argument(0) HttpServletRequest request,
            @Advice.Argument(2) Object handler) {
//        @Advice.Local("otelContext") Context context, //with Local it seem to have issue when there's no same definition for OnMethodExit
//        @Advice.Local("otelScope") Scope scope) {
      Context parentContext = Java8BytecodeBridge.currentContext();
      Span serverSpan = ServerSpan.fromContextOrNull(parentContext);
      if (serverSpan != null) {
        String transactionName = AoSpringWebMvcTracer.spanNameOnHandle(handler);
        serverSpan.setAttribute("TransactionName", transactionName);
      }
    }
  }
}
