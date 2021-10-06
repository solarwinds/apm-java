/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation.annotation;

import com.appoptics.api.ext.LogMethod;
import com.appoptics.api.ext.ProfileMethod;
import com.appoptics.opentelemetry.core.Constants;
import com.appoptics.opentelemetry.core.Util;
import com.tracelytics.joboe.EventValueConverter;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.util.BackTraceUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.tracer.BaseTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.URL;
import java.security.CodeSource;
import java.util.Collections;

public class AppOpticsAnnotationTracer extends BaseTracer {
  private static final AppOpticsAnnotationTracer TRACER = new AppOpticsAnnotationTracer();
  private static EventValueConverter converter = new EventValueConverter();

  public static AppOpticsAnnotationTracer tracer() {
    return TRACER;
  }

  private static final Logger log = LoggerFactory.getLogger(AppOpticsAnnotationTracer.class);

  public Context startSpan(Context parentContext, LogMethod annotation, Method method, SpanKind kind) {

    String operationName = annotation.layer();
    if (operationName == null || operationName.equals("")) {
      operationName = (String) ConfigManager.getConfig(ConfigProperty.AGENT_LAYER);
    }

    operationName = operationName.replace("\"", "\\\"");

    Span span = spanBuilder(parentContext, operationName, kind).startSpan();

    span.setAttribute(Constants.SW_KEY_PREFIX + "Class", method.getDeclaringClass().getName());
    span.setAttribute(Constants.SW_KEY_PREFIX + "Method", method.getName());


    if (annotation.backTrace()) {
      span.setAttribute(Constants.SW_KEY_PREFIX + "Backtrace", BackTraceUtil.backTraceToString(BackTraceUtil.getBackTrace(1)));
    }
    return parentContext.with(span);
  }

  public Context startSpan(Context parentContext, ProfileMethod annotation, Method method, SpanKind kind) {
    String operationName = annotation.profileName();

    operationName = operationName.replace("\"", "\\\"");

    Span span = spanBuilder(parentContext, operationName, kind).startSpan();

    span.setAttribute(Constants.SW_KEY_PREFIX + "Class", method.getDeclaringClass().getName());
    span.setAttribute(Constants.SW_KEY_PREFIX + "FunctionName", method.getName());
    span.setAttribute(Constants.SW_KEY_PREFIX + "Signature", method.toGenericString()); //slightly different from the method signature before, but this is more readable

    if (method.getDeclaringClass().getProtectionDomain() != null) {
      CodeSource codeSource = method.getDeclaringClass().getProtectionDomain().getCodeSource();
      if (codeSource != null) {
        URL location = codeSource.getLocation();
        if (location != null) {
          String file = location.getFile();
          if (file != null && !"".equals(file)) {
            span.setAttribute(Constants.SW_KEY_PREFIX + "File", file);
          }
        }
      }
    }

    if (annotation.backTrace()) {
      span.setAttribute(Constants.SW_KEY_PREFIX + "Backtrace", BackTraceUtil.backTraceToString(BackTraceUtil.getBackTrace(1)));
    }
    return parentContext.with(span);
  }


  @Override
  protected String getInstrumentationName() {
    return "com.appoptics.opentelemetry.annotations";
  }

  public void endMethod(Context context, boolean storeReturn, Object returnValue) {
    if (storeReturn) {
      Span span = Span.fromContext(context);
      Util.setSpanAttributes(span, Collections.singletonMap("ReturnValue", converter.convertToEventValue(returnValue)));
    }
  }

  @Override
  public void end(Context context) {
    super.end(context);
  }
}

