package com.appoptics.opentelemetry.instrumentation;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.api.CallDepth;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.sql.PreparedStatement;
import java.sql.Statement;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge.currentContext;
import static net.bytebuddy.matcher.ElementMatchers.*;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;

public class PreparedStatementInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<ClassLoader> classLoaderOptimization() {
        return hasClassesNamed("java.sql.PreparedStatement");
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return implementsInterface(named("java.sql.PreparedStatement"));
    }

    @Override
    public void transform(TypeTransformer transformer) {
        transformer.applyAdviceToMethod(
                nameStartsWith("set").and(takesArguments(2)).and(isPublic()),
                PreparedStatementInstrumentation.class.getName() + "$PreparedStatementAdvice");
    }

    @SuppressWarnings("unused")
    public static class PreparedStatementAdvice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void onEnter(
                @Advice.This PreparedStatement statement,
                @Advice.Local("otelCallDepth") CallDepth callDepth) {
            // Connection#getMetaData() may execute a Statement or PreparedStatement to retrieve DB info
            // this happens before the DB CLIENT span is started (and put in the current context), so this
            // instrumentation runs again and the shouldStartSpan() check always returns true - and so on
            // until we get a StackOverflowError
            // using CallDepth prevents this, because this check happens before Connection#getMetadata()
            // is called - the first recursive Statement call is just skipped and we do not create a span
            // for it
            callDepth = CallDepth.forClass(Statement.class);
            if (callDepth.getAndIncrement() > 0) {
                return;
            }

        }

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void stopSpan(
                @Advice.Thrown Throwable throwable,
                @Advice.Local("otelCallDepth") CallDepth callDepth) {
            if (callDepth.decrementAndGet() > 0) {
                return;
            }

        }
    }
}
