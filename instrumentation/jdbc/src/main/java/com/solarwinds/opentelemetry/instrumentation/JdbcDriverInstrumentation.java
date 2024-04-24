package com.solarwinds.opentelemetry.instrumentation;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.Properties;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class JdbcDriverInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("java.sql.Driver");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("java.sql.Driver"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        nameStartsWith("connect")
            .and(takesArgument(0, String.class))
            .and(takesArgument(1, Properties.class))
            .and(returns(named("java.sql.Connection"))),
        JdbcDriverInstrumentation.class.getName() + "$DriverAdvice");
  }

  @SuppressWarnings("unused")
  public static class DriverAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void addDbInfo(@Advice.Argument(0) String connectionUrl) {
      String jdbcUrl;
      if (connectionUrl.startsWith("jdbc:")) {
        jdbcUrl = connectionUrl.substring("jdbc:".length());

      } else if (connectionUrl.startsWith("jdbc-secretsmanager:")) {
        jdbcUrl = connectionUrl.substring("jdbc-secretsmanager:".length());

      } else {
        return;
      }

      int typeLoc = jdbcUrl.indexOf(':');
      if (typeLoc < 1) {
        return;
      }

      String type = jdbcUrl.substring(0, typeLoc);
      TraceContextInjector.addDb(type.toLowerCase());
    }
  }
}
