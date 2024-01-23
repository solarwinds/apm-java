/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.*;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/** Experimental instrumentation to set `TransactionName` KV to OT Trace root span for Spring MVC */
public class SwoHandlerAdapterInstrumentation implements TypeInstrumentation {

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
        SwoHandlerAdapterInstrumentation.class.getName() + "$ControllerAdvice");
  }

  public static class ControllerAdvice {
    @Advice.OnMethodEnter
    public static void setTransactionNameToServerSpan(@Advice.Argument(2) Object handler) {
      Context parentContext = Java8BytecodeBridge.currentContext();
      Span serverSpan = Java8BytecodeBridge.spanFromContext(parentContext);
      if (serverSpan != null) {
        String transactionName = SwoSpringWebMvcTracer.spanNameOnHandle(handler);
        serverSpan.setAttribute("HandlerName", transactionName);
      }
    }
  }
}
