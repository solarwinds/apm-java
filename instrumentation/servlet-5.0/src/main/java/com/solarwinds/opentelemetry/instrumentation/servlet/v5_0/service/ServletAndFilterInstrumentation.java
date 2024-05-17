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

package com.solarwinds.opentelemetry.instrumentation.servlet.v5_0.service;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class ServletAndFilterInstrumentation implements TypeInstrumentation {
  private final String basePackageName;
  private final String adviceClassName;
  private final String servletInitAdviceClassName;
  private final String filterInitAdviceClassName;

  public ServletAndFilterInstrumentation(
      String basePackageName,
      String adviceClassName,
      String servletInitAdviceClassName,
      String filterInitAdviceClassName) {
    this.basePackageName = basePackageName;
    this.adviceClassName = adviceClassName;
    this.servletInitAdviceClassName = servletInitAdviceClassName;
    this.filterInitAdviceClassName = filterInitAdviceClassName;
  }

  public ServletAndFilterInstrumentation(String basePackageName, String adviceClassName) {
    this(basePackageName, adviceClassName, null, null);
  }

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed(basePackageName + ".Servlet");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return hasSuperType(namedOneOf(basePackageName + ".Filter", basePackageName + ".Servlet"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        namedOneOf("doFilter", "service")
            .and(takesArgument(0, named(basePackageName + ".ServletRequest")))
            .and(takesArgument(1, named(basePackageName + ".ServletResponse")))
            .and(isPublic()),
        adviceClassName);
    if (servletInitAdviceClassName != null) {
      transformer.applyAdviceToMethod(
          named("init").and(takesArgument(0, named(basePackageName + ".ServletConfig"))),
          servletInitAdviceClassName);
    }
    if (filterInitAdviceClassName != null) {
      transformer.applyAdviceToMethod(
          named("init").and(takesArgument(0, named(basePackageName + ".FilterConfig"))),
          filterInitAdviceClassName);
    }
  }
}
