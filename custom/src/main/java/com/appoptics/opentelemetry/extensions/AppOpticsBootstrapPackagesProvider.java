package com.appoptics.opentelemetry.extensions;

import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.extension.bootstrap.BootstrapPackagesBuilder;
import io.opentelemetry.javaagent.extension.bootstrap.BootstrapPackagesConfigurer;

/**
 * This adds the Joboe core classes to the list which would always be loaded by the bootstrap classloader,
 * no matter what classloader is used initially.
 * The Otel agent instruments all classloaders and checks the class named to be loaded. It will load the class
 * with the bootstrap classloader if the class mateches any of the prefix in the list above.
 */
@AutoService(BootstrapPackagesConfigurer.class)
public class AppOpticsBootstrapPackagesProvider implements BootstrapPackagesConfigurer {
    @Override
    public void configure(Config config, BootstrapPackagesBuilder builder) {
        builder.add("com.tracelytics");
    }
}
