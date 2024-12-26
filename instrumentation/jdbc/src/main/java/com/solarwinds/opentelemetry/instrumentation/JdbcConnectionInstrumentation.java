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

package com.solarwinds.opentelemetry.instrumentation;

import static com.solarwinds.opentelemetry.instrumentation.jdbc.shared.DbConstraintChecker.isDbConfigured;
import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.returns;

import com.solarwinds.opentelemetry.instrumentation.jdbc.shared.DbConstraintChecker;
import io.opentelemetry.javaagent.bootstrap.CallDepth;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class JdbcConnectionInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("java.sql.Connection");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    if (DbConstraintChecker.preparedSqlTagEnabled()) {
      // Duplicating lines 51 - 62 across TypeInstrumentation impls due to runtime muzzle mismatch
      // as a result of missing `net.bytebuddy.matcher.*` classes in consolidated method.
      ElementMatcher.Junction<TypeDescription> matcher = null;
      if (isDbConfigured(DbConstraintChecker.Db.mysql)) {
        matcher = nameStartsWith("com.mysql.cj.jdbc"); // only inject MySQL JDBC driver
      }

      if (isDbConfigured(DbConstraintChecker.Db.postgresql)) {
        if (matcher != null) {
          matcher = matcher.or(nameStartsWith("org.postgresql"));
        } else {
          matcher = nameStartsWith("org.postgresql");
        }
      }

      if (matcher != null) {
        return matcher.and(implementsInterface(named("java.sql.Connection")));
      }
    }

    return none();
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("prepareStatement")
            .and(returns(implementsInterface(named("java.sql.PreparedStatement")))),
        JdbcConnectionInstrumentation.class.getName() + "$PrepareAdvice");
  }

  @SuppressWarnings("unused")
  public static class PrepareAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void start(
        @Advice.Local("swoCallDepth") CallDepth callDepth,
        @Advice.Argument(value = 0, readOnly = false) String sql) {

      callDepth = CallDepth.forClass(TraceContextInjector.class);
      if (callDepth.getAndIncrement() > 0) {
        return;
      }

      sql = TraceContextInjector.inject(currentContext(), sql);
      StatementTracer.writeQuerySpec(currentContext());
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void end(@Advice.Local("swoCallDepth") CallDepth callDepth) {
      if (callDepth != null) {
        callDepth.decrementAndGet();
      }
    }
  }
}
