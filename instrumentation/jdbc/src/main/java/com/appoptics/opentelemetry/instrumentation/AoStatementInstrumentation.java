/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.api.CallDepthThreadLocalMap;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.sql.Statement;
import java.util.Map;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static io.opentelemetry.javaagent.extension.matcher.ClassLoaderMatcher.hasClassesNamed;
import static io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge.currentContext;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.*;

public class AoStatementInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<ClassLoader> classLoaderOptimization() {
        return hasClassesNamed("java.sql.Statement");
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return implementsInterface(named("java.sql.Statement"));
    }

    @Override
    public void transform(TypeTransformer transformer) {
        transformer.applyAdviceToMethod(
                nameStartsWith("execute").and(takesArgument(0, String.class)).and(isPublic()),
                AoStatementInstrumentation.class.getName() + "$StatementAdvice");
    }

//  @Override
//  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
//    return singletonMap(
//            nameStartsWith("execute").and(takesArgument(0, String.class)).and(isPublic()),
//            AoStatementInstrumentation.class.getName() + "$StatementAdvice");
//  }

      public static class StatementAdvice {
          //    @Advice.OnMethodEnter//(suppress = Throwable.class)
//    public static void onEnter(
//            @Advice.Argument(0) String sql,
//            @Advice.This Statement statement) {
//      if (CallDepthThreadLocalMap.getCallDepth(Statement.class).get() != 1) { //only report back when depth is one to avoid duplications
//        return;
//      }
//
//      AoStatementTracer.writeStackTrace(Context.current());
//    }
          @Advice.OnMethodEnter(suppress = Throwable.class)
          public static void onEnter(
                  @Advice.Argument(0) String sql,
                  @Advice.This Statement statement) {
              try {
                  System.out.println("!!!!!!!!!!!!!!!!??????????????? :X");
                  if (CallDepthThreadLocalMap.getCallDepth(Statement.class).get() != 1) { //only report back when depth is one to avoid duplications
                      return;
                  }


                  AoStatementTracer.writeStackTrace(Context.current());
              } catch (Throwable e) {
                  System.out.println(":( !!!!!!!!!!!!!!!");
                  e.printStackTrace();
              }
          }

          @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
          public static void stopSpan(
                  @Advice.Thrown Throwable throwable) {

              System.out.println("????????????? :D ~");
//              if (scope == null) {
//                  return;
//              }
//              CallDepthThreadLocalMap.reset(Statement.class);

              //scope.close();
          }
      }
}
