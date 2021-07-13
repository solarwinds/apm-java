/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation.annotation;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Experimental instrumentation to process @LogMethod and @ProfileMethod usage from
 * our AO/OT SDK
 */
@AutoService(InstrumentationModule.class)
public class AppOpticsAnnotationModule extends InstrumentationModule {

  public AppOpticsAnnotationModule() {
    super("appoptics-annotations");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Arrays.asList(new AnnotatedLogMethodInstrumentation(), new AnnotatedProfileMethodInstrumentation());
  }


  @Override
  public boolean isHelperClass(String className) {
    return className.startsWith("com.appoptics.opentelemetry.");
  }

  public static class AnnotatedLogMethodInstrumentation implements TypeInstrumentation {
    private final ElementMatcher.Junction<AnnotationSource> annotatedMethodMatcher;

    AnnotatedLogMethodInstrumentation() {
      annotatedMethodMatcher =
          isAnnotatedWith(named("com.appoptics.api.ext.LogMethod"));
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return declaresMethod(annotatedMethodMatcher);
    }

    @Override
    public void transform(TypeTransformer transformer) {
      transformer.applyAdviceToMethod(
              annotatedMethodMatcher,
              "com.appoptics.opentelemetry.instrumentation.annotation.AppOpticsLogMethodAnnotationAdvice");
    }
  }

  public static class AnnotatedProfileMethodInstrumentation implements TypeInstrumentation {
    private final ElementMatcher.Junction<AnnotationSource> annotatedMethodMatcher;

    AnnotatedProfileMethodInstrumentation() {
      annotatedMethodMatcher =
              isAnnotatedWith(named("com.appoptics.api.ext.ProfileMethod"));
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return declaresMethod(annotatedMethodMatcher);
    }

    @Override
    public void transform(TypeTransformer transformer) {
      transformer.applyAdviceToMethod(
              annotatedMethodMatcher,
              "com.appoptics.opentelemetry.instrumentation.annotation.AppOpticsProfileMethodAnnotationAdvice");
    }
  }
}
