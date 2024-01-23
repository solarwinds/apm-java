package com.appoptics.opentelemetry.extensions;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.bootstrap.BootstrapPackagesBuilder;
import io.opentelemetry.javaagent.tooling.bootstrap.BootstrapPackagesConfigurer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

/**
 * This adds the Joboe core classes to the list which would always be loaded by the bootstrap
 * classloader, no matter what classloader is used initially. The Otel agent instruments all
 * classloaders and checks the class named to be loaded. It will load the class with the bootstrap
 * classloader if the class mateches any of the prefix in the list above.
 */
@AutoService(BootstrapPackagesConfigurer.class)
public class SolarwindsBootstrapPackagesProvider implements BootstrapPackagesConfigurer {
  @Override
  public void configure(BootstrapPackagesBuilder builder, ConfigProperties config) {
    builder.add("com.solarwinds.joboe");
  }
}
