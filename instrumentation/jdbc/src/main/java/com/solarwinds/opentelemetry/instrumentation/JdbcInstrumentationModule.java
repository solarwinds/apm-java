/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.solarwinds.opentelemetry.instrumentation;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.ArrayList;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class JdbcInstrumentationModule extends InstrumentationModule {
  public JdbcInstrumentationModule() {
    super("sw-jdbc", "solarwinds-jdbc");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    List<TypeInstrumentation> typeInstrumentations = new ArrayList<>();
    typeInstrumentations.add(new JdbcStatementInstrumentation());

    typeInstrumentations.add(new JdbcConnectionInstrumentation());
    typeInstrumentations.add(new JdbcPreparedStatementInstrumentation());
    return typeInstrumentations;
  }

  @Override
  public int order() {
    return 1;
  }

  @Override
  public boolean isHelperClass(String className) {
    return className.startsWith("com.solarwinds.opentelemetry.instrumentation");
  }
}
