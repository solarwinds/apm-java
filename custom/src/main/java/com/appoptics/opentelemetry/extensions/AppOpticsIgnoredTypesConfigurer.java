package com.appoptics.opentelemetry.extensions;

import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.extension.ignore.IgnoredTypesBuilder;
import io.opentelemetry.javaagent.extension.ignore.IgnoredTypesConfigurer;

@AutoService(IgnoredTypesConfigurer.class)
public class AppOpticsIgnoredTypesConfigurer implements IgnoredTypesConfigurer {
    @Override
    public void configure(Config config, IgnoredTypesBuilder builder) {
        builder.ignoreClass("com.appoptics.ext.");
    }
}
