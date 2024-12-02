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

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.none;

import com.google.auto.service.AutoService;
import com.solarwinds.opentelemetry.instrumentation.jdbc.shared.DbConstraintChecker;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class HibernateInstrumentationModule extends InstrumentationModule {

  public HibernateInstrumentationModule() {
    super("hibernate", "sw-hibernate-6.0");
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    if (DbConstraintChecker.preparedSqlTagEnabled() && DbConstraintChecker.anyDbConfigured()) {
      return hasClassesNamed(
          // not present before 6.0
          "org.hibernate.query.spi.SqmQuery");
    }
    return none();
  }

  @Override
  public boolean isHelperClass(String className) {
    return className.startsWith("com.solarwinds.opentelemetry.instrumentation");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return List.of(new DrsaInstrumentation());
  }

  @Override
  public int order() {
    return Integer.MAX_VALUE;
  }
}