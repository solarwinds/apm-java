package com.appoptics.opentelemetry.instrumentation;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.javaagent.bootstrap.CallDepth;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public class AoPreparedStatementInstrumentation implements TypeInstrumentation {

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
                AoPreparedStatementInstrumentation.class.getName() + "$PreparedStatementSetAdvice");

        transformer.applyAdviceToMethod(
                nameStartsWith("execute").and(takesArguments(0)).and(isPublic()),
                AoPreparedStatementInstrumentation.class.getName() + "$PreparedStatementExecuteAdvice");
    }

    @SuppressWarnings("unused")
    public static class PreparedStatementSetAdvice {
        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void onEnter(
                @Advice.This PreparedStatement statement,
                @Advice.Argument(value = 0, readOnly = true) int index,
                @Advice.Argument(value = 1, readOnly = true) Object value) {
            // Connection#getMetaData() may execute a Statement or PreparedStatement to retrieve DB info
            // this happens before the DB CLIENT span is started (and put in the current context), so this
            // instrumentation runs again and the shouldStartSpan() check always returns true - and so on
            // until we get a StackOverflowError
            // using CallDepth prevents this, because this check happens before Connection#getMetadata()
            // is called - the first recursive Statement call is just skipped and we do not create a span
            // for it
            if (CallDepth.forClass(Statement.class).getAndIncrement() != 0) { //only report back when depth is one to avoid duplications
                CallDepth.forClass(Statement.class).decrementAndGet();
                return;
            }
            QueryArgsCollector.collect(statement, currentContext(), index, value);
            CallDepth.forClass(Statement.class).decrementAndGet(); // do not want to interfere with the Otel's instrumentation
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void onExit(
                @Advice.Thrown Throwable throwable) {
            // we have to have this empty exit advice as otherwise local parameters in the enter method are not supported.
        }
    }

    public static class QueryArgsAttributeKey {
        public static final AttributeKey<List<String>> KEY = AttributeKey.stringArrayKey("QueryArgs");
    }

    @SuppressWarnings("unused")
    public static class PreparedStatementExecuteAdvice {
        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void onEnter(
                @Advice.This PreparedStatement statement) {

        }

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void onExit(
                @Advice.This PreparedStatement statement,
                @Advice.Thrown Throwable throwable) {
            if (CallDepth.forClass(Statement.class).getAndIncrement() != 1) { //only report back when depth is one to avoid duplications
                // Note that we need to decrement the call depth counter at every branch, otherwise the JDBC instrumentation of the
                // Otel agent will break.
                CallDepth.forClass(Statement.class).decrementAndGet();
                return;
            }
            QueryArgsCollector.maybeAttach(statement, currentContext());
            StatementTruncator.maybeTruncateStatement(currentContext());
            CallDepth.forClass(Statement.class).decrementAndGet(); // do not want to interfere with the Otel's instrumentation
        }
    }
}
