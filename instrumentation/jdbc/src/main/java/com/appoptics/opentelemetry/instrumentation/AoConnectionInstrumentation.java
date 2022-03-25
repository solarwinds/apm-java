package com.appoptics.opentelemetry.instrumentation;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge.currentContext;
import static net.bytebuddy.matcher.ElementMatchers.*;

import com.appoptics.opentelemetry.core.Constants;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.util.BackTraceUtil;
import io.opentelemetry.api.trace.Span;
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
        Boolean sqlTag = ConfigManager.getConfigOptional(ConfigProperty.AGENT_SQL_TAG, false);
        if (!sqlTag) {
            return none();
        }
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
            sql = TraceContextInjector.inject(currentContext(), sql);

            Span span = Span.fromContext(currentContext());
            if (span.getSpanContext().isSampled()) {
                String backTraceString = BackTraceUtil.backTraceToString(BackTraceUtil.getBackTrace(1));
                span.setAttribute(Constants.SW_KEY_PREFIX + "Backtrace", backTraceString);
                span.setAttribute(Constants.SW_KEY_PREFIX + "Spec", "query");
            }
        }
    }
}

