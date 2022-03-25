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
public class AoJdbcInstrumentationModule extends InstrumentationModule {
  public AoJdbcInstrumentationModule() {
    super("ao-jdbc", "ao-spring-webmvc-3.1");
  }


  //TODO as documented in https://github.com/appoptics/appoptics-opentelemetry-java/issues/5. Expanding this list does NOT work
//  @Override
//  protected String[] additionalHelperClassNames() {
//    return new String [] { AoStatementTracer.class.getName() };
//  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(
            new AoConnectionInstrumentation(),
            new AoStatementInstrumentation(),
            new PreparedStatementInstrumentation()
    );
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
