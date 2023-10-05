/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;

import java.util.ArrayList;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class AoJdbcInstrumentationModule extends InstrumentationModule {
  public AoJdbcInstrumentationModule() {
    super("sw-jdbc", "solarwinds-jdbc");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    List<TypeInstrumentation> typeInstrumentations = new ArrayList<>();
    typeInstrumentations.add(new AoStatementInstrumentation());

    typeInstrumentations.add(new AoConnectionInstrumentation());
    typeInstrumentations.add(new AoPreparedStatementInstrumentation());
    return typeInstrumentations;
  }

  @Override
  public int order() {
    return 1;
  }

  @Override
  public boolean isHelperClass(String className) {
    return className.startsWith("com.appoptics.opentelemetry.instrumentation");
  }
}
