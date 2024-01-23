/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation;

import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class SwoSpringWebMvcInstrumentationModule extends InstrumentationModule {
  public SwoSpringWebMvcInstrumentationModule() {
    super("ao-spring-webmvc", "ao-spring-webmvc-3.1");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(new SwoHandlerAdapterInstrumentation());
  }

  @Override
  public boolean isHelperClass(String className) {
    return className.startsWith("com.appoptics.opentelemetry.instrumentation");
  }
}
