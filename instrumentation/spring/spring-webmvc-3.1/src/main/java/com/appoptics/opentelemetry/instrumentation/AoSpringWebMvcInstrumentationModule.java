/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;

import java.util.List;

import static java.util.Arrays.asList;

@AutoService(InstrumentationModule.class)
public class AoSpringWebMvcInstrumentationModule extends InstrumentationModule {
  public AoSpringWebMvcInstrumentationModule() {
    super("ao-spring-webmvc", "ao-spring-webmvc-3.1");
  }

//  @Override
//  protected String[] additionalHelperClassNames() {
//    return new String [] {AoSpringWebMvcTracer.class.getName() };
//  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(
        new AoHandlerAdapterInstrumentation()
    );
  }

  @Override
  public boolean isHelperClass(String className) {
    return className.startsWith("com.appoptics.") || className.startsWith("com.tracelytics.");
  }
}
