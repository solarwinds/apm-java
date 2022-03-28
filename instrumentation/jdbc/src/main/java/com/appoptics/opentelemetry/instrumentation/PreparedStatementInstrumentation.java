package com.appoptics.opentelemetry.instrumentation;

import com.tracelytics.joboe.XTraceOptions;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.api.CallDepth;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

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
                PreparedStatementInstrumentation.class.getName() + "$PreparedStatementSetAdvice");

        transformer.applyAdviceToMethod(
                nameStartsWith("execute").and(takesArguments(0)).and(isPublic()),
                PreparedStatementInstrumentation.class.getName() + "$PreparedStatementExecuteAdvice");
    }

    @SuppressWarnings("unused")
    public static class PreparedStatementSetAdvice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void onEnter(
                @Advice.This PreparedStatement statement,
                @Advice.Argument(value = 0, readOnly = false) int index,
                @Advice.Argument(value = 1, readOnly = false) Object value,
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
            Context context = currentContext();
            SortedMap<String, String> queryArgs = context.get(QueryArgsContextKey.KEY);
            if (queryArgs == null) {
                queryArgs = new TreeMap<>();
                context.with(QueryArgsContextKey.KEY, queryArgs).makeCurrent();
            }
            queryArgs.put(String.valueOf(index), JdbcEventValueConverter.convert(value).toString()); // TODO
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
    public static class QueryArgsContextKey {
        public static final ContextKey<SortedMap<String, String>> KEY = ContextKey.named("query-args-key");
        private QueryArgsContextKey() {}
    }

    public static class QueryArgsAttributeKey {
        public static final AttributeKey<List<String>> KEY = AttributeKey.stringArrayKey("QueryArgs");
    }
    public static class PreparedStatementExecuteAdvice {
        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void stopSpan(
                @Advice.Thrown Throwable throwable,
                @Advice.Local("otelCallDepth") CallDepth callDepth) {
            if (callDepth.decrementAndGet() > 0) {
                return;
            }
            Context context = currentContext();
            Span span = Span.fromContext(context);
            SpanContext spanContext = span.getSpanContext();
            if (!(spanContext.isValid() && spanContext.isSampled())) {
                SortedMap<String, String> argsMap = context.get(QueryArgsContextKey.KEY);

                if (argsMap != null) {
                    List<String> queryArgs = new ArrayList<>(argsMap.values());
                    span.setAttribute(QueryArgsAttributeKey.KEY, queryArgs);
                }
            }

            StatementTruncator.maybeTruncateStatement(span);
        }
    }
}
