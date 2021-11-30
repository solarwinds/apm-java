/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.appoptics.opentelemetry.instrumentation.servlet.v5_0;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import com.appoptics.opentelemetry.instrumentation.servlet.common.service.ServletAndFilterInstrumentation;
import java.util.Arrays;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class JakartaServletAddonInstrumentationModule extends InstrumentationModule {
    private static final String BASE_PACKAGE = "jakarta.servlet";

    public JakartaServletAddonInstrumentationModule() {
        super("servletAddon", "servlet-5.0-Addon");
    }

    @Override
    public List<TypeInstrumentation> typeInstrumentations() {
        return Arrays.asList(
                new ServletAndFilterInstrumentation(
                        BASE_PACKAGE,
                        adviceClassName(".service.JakartaServletServiceAdvice")));
    }

    private static String adviceClassName(String suffix) {
        return JakartaServletAddonInstrumentationModule.class.getPackage().getName() + suffix;
    }

    @Override
    public boolean isHelperClass(String className) {
        return className.startsWith("com.appoptics.opentelemetry.instrumentation.servlet.v5_0");
    }
}
