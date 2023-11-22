/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation;

import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * Experimental instrumentation to add back traces to existing OT spans.
 *
 * <p>This only works for `Statement` at this moment (ie no `PreparedStatement`)
 */
public class AoStatementInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("java.sql.Statement");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    Boolean sqlTag = ConfigManager.getConfigOptional(ConfigProperty.AGENT_SQL_TAG, false);
    if (sqlTag) {
      return implementsInterface(named("java.sql.Statement"));
    }

    return none();
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
      sql = TraceContextInjector.inject(currentContext(), sql);
      AoStatementTracer.writeStackTraceSpec(currentContext());
      StatementTruncator.maybeTruncateStatement(currentContext());
    }
  }
}
