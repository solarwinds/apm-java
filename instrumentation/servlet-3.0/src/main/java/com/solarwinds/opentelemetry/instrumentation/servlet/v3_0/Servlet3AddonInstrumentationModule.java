/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.solarwinds.opentelemetry.instrumentation.servlet.v3_0;

import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class Servlet3AddonInstrumentationModule extends InstrumentationModule {
  private static final String BASE_PACKAGE = "javax.servlet";

  public Servlet3AddonInstrumentationModule() {
    super("servletAddon", "servlet-3.0-Addon");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(
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
