package com.appoptics.opentelemetry.instrumentation;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge.currentContext;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class AoConnectionInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<ClassLoader> classLoaderOptimization() {
        return hasClassesNamed("java.sql.Connection");
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return named("com.mysql.cj.jdbc.ConnectionImpl") // only inject MySQL JDBC driver
                .and(implementsInterface(named("java.sql.Connection")));
    }

    @Override
    public void transform(TypeTransformer transformer) {
        transformer.applyAdviceToMethod(
                nameStartsWith("prepare")
                        .and(takesArgument(0, String.class))
                        // Also include CallableStatement, which is a sub type of PreparedStatement
                        .and(returns(implementsInterface(named("java.sql.PreparedStatement")))),
                AoConnectionInstrumentation.class.getName() + "$PrepareAdvice");
    }

    @SuppressWarnings("unused")
    public static class PrepareAdvice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void injectComment(
                @Advice.Argument(value = 0, readOnly = false) String sql) {
            sql = injectTraceContext(currentContext(), sql);
        }

        public static String injectTraceContext(Context context, String sql) {
            if (sql.contains("traceparent")) {
                return sql;
            }

            SpanContext spanContext = Span.fromContext(context).getSpanContext();
            if (!spanContext.isValid()) {
                return sql;
            }
            String flags = spanContext.isSampled() ? "01" : "00";
            String traceContext = "00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-" + flags;

            return String.format("/*traceparent:'%s'*/ %s", traceContext, sql);
        }
    }
}

