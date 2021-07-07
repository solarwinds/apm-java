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

@AutoService(InstrumentationModule.class)
public class AppOpticsAnnotationModule extends InstrumentationModule {

  public AppOpticsAnnotationModule() {
    super("appoptics-annotations");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Arrays.asList(new AnnotatedLogMethodInstrumentation(), new AnnotatedProfileMethodInstrumentation());
  }

//  @Override
//  public boolean isHelperClass(String className) {
//    System.out.println("Checking???????????????????????? " + className);
//    return className != null && className.startsWith("com.tracelytics") || className.startsWith("com.appoptics");
//  }

//  @Override
//  protected String[] additionalHelperClassNames() {
//    return new String [] {
//            AppOpticsAnnotationTracer.class.getName(),
//            EventValueConverter.class.getName(),
//            LoggerFactory.class.getName(),
//            Logger.class.getName(),
//            Logger.Level.class.getName(),
//            "com.tracelytics.logging.Logger$1",
//            LogSetting.class.getName(),
//            "com.tracelytics.logging.Logger$LoggerStream",
//            "com.tracelytics.logging.Logger$SystemOutStream",
//            "com.tracelytics.logging.Logger$SystemErrStream",
//            "com.tracelytics.joboe.EventValueConverter$Converter",
//
//            "com.tracelytics.joboe.EventValueConverter$SimpleParameterHandler",
//            "com.tracelytics.joboe.EventValueConverter$ByteParameterHandler",
//            "com.tracelytics.joboe.EventValueConverter$ShortParameterHandler",
//            "com.tracelytics.joboe.EventValueConverter$FloatParameterHandler",
//            "com.tracelytics.joboe.EventValueConverter$BigIntegerParameterHandler",
//            "com.tracelytics.joboe.EventValueConverter$BigDecimalParameterHandler",
//            "com.tracelytics.joboe.EventValueConverter$MapParameterHandler",
//            "com.tracelytics.joboe.EventValueConverter$CollectionParameterHandler",
//            "com.tracelytics.joboe.EventValueConverter$ByteArrayParameterHandler",
//            "com.tracelytics.joboe.EventValueConverter$ClassNameParameterHandler",
//            "com.tracelytics.joboe.EventValueConverter$ToStringParameterHandler",
//
//            "com.tracelytics.util.BackTraceUtil",
//            BackTraceCache.class.getName(),
//
//
//    };
//  }


  //TODO this does NOT work as it finds too many classes and trigger exception. See https://github.com/appoptics/appoptics-opentelemetry-java/pull/5#issue-668569209
//  @Override
//  public boolean isHelperClass(String className) {
//    return (!className.startsWith("com.appoptics.ext") || className.startsWith("com.appoptics.ext.google"))
//    && (className.startsWith("com.appoptics.") || className.startsWith("com.tracelytics."));
//  }
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
