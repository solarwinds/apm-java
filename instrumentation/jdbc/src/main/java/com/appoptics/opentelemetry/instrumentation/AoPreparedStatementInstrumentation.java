package com.appoptics.opentelemetry.instrumentation;

import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.sql.PreparedStatement;
import java.util.List;

import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.*;

public class AoPreparedStatementInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<ClassLoader> classLoaderOptimization() {
        return hasClassesNamed("java.sql.PreparedStatement");
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        Boolean sqlTagPrepared = ConfigManager.getConfigOptional(ConfigProperty.AGENT_SQL_TAG_PREPARED, false);
        if (sqlTagPrepared) {
            return implementsInterface(named("java.sql.PreparedStatement"));
        }
        return none();
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
            StatementTruncator.maybeTruncateStatement(currentContext());
        }
    }
}
