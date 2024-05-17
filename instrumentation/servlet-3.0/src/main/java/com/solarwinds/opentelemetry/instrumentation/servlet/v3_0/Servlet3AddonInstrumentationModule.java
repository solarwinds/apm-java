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

package com.solarwinds.opentelemetry.instrumentation.servlet.v3_0;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.Collections;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class Servlet3AddonInstrumentationModule extends InstrumentationModule {
  private static final String BASE_PACKAGE = "javax.servlet";

  public Servlet3AddonInstrumentationModule() {
    super("servletAddon", "servlet-3.0-Addon");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(
        new ServletAndFilterInstrumentation(BASE_PACKAGE, adviceClassName(".Servlet3Advice")));
  }

  private static String adviceClassName(String suffix) {
    return Servlet3AddonInstrumentationModule.class.getPackage().getName() + suffix;
  }

  @Override
  public boolean isHelperClass(String className) {
    return className.startsWith("com.solarwinds.opentelemetry.instrumentation.servlet.v3_0");
  }
}
