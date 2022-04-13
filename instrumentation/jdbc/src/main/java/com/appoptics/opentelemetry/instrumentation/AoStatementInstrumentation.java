/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.api.CallDepth;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.sql.Statement;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge.currentContext;
import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Experimental instrumentation to add back traces to existing OT spans.
 *
 * This only works for `Statement` at this moment (ie no `PreparedStatement`)
 */
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

    @SuppressWarnings("unused")
    public static class StatementAdvice {
        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Argument(value = 0, readOnly = false) String sql) {
            if (CallDepth.forClass(Statement.class).get() != 1) { //only report back when depth is one to avoid duplications
                return;
            }
            sql = TraceContextInjector.inject(currentContext(), sql);
            AoStatementTracer.writeStackTraceSpec(currentContext());
            StatementTruncator.maybeTruncateStatement(currentContext());
        }
    }

}
