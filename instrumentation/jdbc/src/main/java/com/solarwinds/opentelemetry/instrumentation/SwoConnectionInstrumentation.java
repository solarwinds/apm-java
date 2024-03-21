package com.solarwinds.opentelemetry.instrumentation;

import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class SwoConnectionInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("java.sql.Connection");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    Boolean sqlTagPrepared =
        ConfigManager.getConfigOptional(ConfigProperty.AGENT_SQL_TAG_PREPARED, false);
    if (sqlTagPrepared) {
      return named("com.mysql.cj.jdbc.ConnectionImpl") // only inject MySQL JDBC driver
          .and(implementsInterface(named("java.sql.Connection")));
    }

    return none();
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        nameStartsWith("prepare")
            .and(takesArgument(0, String.class))
            // Also include CallableStatement, which is a subtype of PreparedStatement
            .and(returns(implementsInterface(named("java.sql.PreparedStatement")))),
        SwoConnectionInstrumentation.class.getName() + "$PrepareAdvice");
  }

  @SuppressWarnings("unused")
  public static class PrepareAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void injectComment(@Advice.Argument(value = 0, readOnly = false) String sql) {
      sql = TraceContextInjector.inject(currentContext(), sql);
      SwoStatementTracer.writeStackTraceSpec(currentContext());
    }
  }
}
