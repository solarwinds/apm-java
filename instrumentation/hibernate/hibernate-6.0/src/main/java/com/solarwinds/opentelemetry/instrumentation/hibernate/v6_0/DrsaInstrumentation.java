/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.solarwinds.opentelemetry.instrumentation.hibernate.v6_0;

import static com.solarwinds.opentelemetry.instrumentation.hibernate.v6_0.Commenter.generateComment;
import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.bootstrap.CallDepth;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.lang.reflect.Field;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hibernate.sql.results.jdbc.internal.DeferredResultSetAccess;

public class DrsaInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("org.hibernate.sql.results.jdbc.internal.DeferredResultSetAccess");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("org.hibernate.sql.results.jdbc.internal.DeferredResultSetAccess");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod().and(named("getResultSet")),
        DrsaInstrumentation.class.getName() + "$DeferredResultSetAccessAdvice");
  }

  @SuppressWarnings("unused")
  public static class DeferredResultSetAccessAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void startMethod(
        @Advice.This DeferredResultSetAccess drsa,
        @Advice.Local("swoCallDepth") CallDepth callDepth,
        @Advice.Local("swoSqlContext") String swoSql,
        @Advice.Local("swoContext") Context context,
        @Advice.Local("swoScope") Scope scope) {
      callDepth = CallDepth.forClass(DeferredResultSetAccess.class);
      if (callDepth.getAndIncrement() > 0) {
        return;
      }

      try {
        /*
        We're using reflection here because this seems to be the best place we can intercept the raw sql before the
        PreparedStatement is generated. The generation of the prepared statement in done via a Java 8 lambda which
        can be tricky to bytecode modify. The consequence here is that we're accessing internal API, and it's bound to
        be flaky.
        */

        Class<?> clazz = drsa.getClass();
        Field privateField = clazz.getDeclaredField("finalSql");
        privateField.setAccessible(true);

        String queryString = (String) privateField.get(drsa);
        if (queryString.contains("traceparent")) {
          return;
        }

        Context parentContext = currentContext();
        if (!HibernateInstrumenter.getInstance().shouldStart(parentContext, queryString)) {
          return;
        }

        context = HibernateInstrumenter.getInstance().start(parentContext, queryString);
        scope = context.makeCurrent();
        String comment = generateComment(currentContext());

        if (comment != null) {
          privateField.set(drsa, String.format("%s %s", comment, queryString));
        }

        swoSql = queryString;

      } catch (Throwable ignore) {
        // ignore because we can't do jack here
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void endMethod(
        @Advice.Thrown Throwable throwable,
        @Advice.Local("swoCallDepth") CallDepth callDepth,
        @Advice.Local("swoSqlContext") String swoSql,
        @Advice.Local("swoContext") Context context,
        @Advice.Local("swoScope") Scope scope) {

      if (callDepth == null || callDepth.decrementAndGet() > 0) {
        return;
      }

      if (scope != null) {
        scope.close();
        HibernateInstrumenter.getInstance().end(context, swoSql, null, throwable);
      }
    }
  }
}
