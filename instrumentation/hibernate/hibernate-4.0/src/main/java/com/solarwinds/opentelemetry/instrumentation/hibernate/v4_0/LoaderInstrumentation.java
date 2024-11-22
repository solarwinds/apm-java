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
package com.solarwinds.opentelemetry.instrumentation.hibernate.v4_0;

import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.bootstrap.CallDepth;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.loader.Loader;

public class LoaderInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("org.hibernate.Query");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return hasSuperType(named("org.hibernate.loader.Loader"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod().and(named("doQuery")), LoaderInstrumentation.class.getName() + "$LoaderAdvice");
  }

  @SuppressWarnings("unused")
  public static class LoaderAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void startMethod(
        @Advice.Argument(1) QueryParameters queryParameters,
        @Advice.Local("swoCallDepth") CallDepth callDepth,
        @Advice.Local("swoContext") Context context,
        @Advice.Local("swoScope") Scope scope) {

      callDepth = CallDepth.forClass(Loader.class);
      if (callDepth.getAndIncrement() > 0) {
        return;
      }

      String sql = queryParameters.getFilteredSQL();
      Context parentContext = currentContext();
      if (!InstrumenterSingleton.instrumenter().shouldStart(parentContext, sql)) {
        return;
      }

      context = InstrumenterSingleton.instrumenter().start(parentContext, sql);
      scope = context.makeCurrent();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void endMethod(
        @Advice.Thrown Throwable throwable,
        @Advice.Argument(1) QueryParameters queryParameters,
        @Advice.Local("swoCallDepth") CallDepth callDepth,
        @Advice.Local("swoContext") Context context,
        @Advice.Local("swoScope") Scope scope) {

      if (callDepth == null || callDepth.decrementAndGet() > 0) {
        return;
      }

      if (scope != null) {
        scope.close();
        InstrumenterSingleton.instrumenter()
            .end(context, queryParameters.getFilteredSQL(), null, throwable);
      }
    }
  }
}
