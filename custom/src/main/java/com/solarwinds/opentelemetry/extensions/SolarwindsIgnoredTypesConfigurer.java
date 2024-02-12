package com.solarwinds.opentelemetry.extensions;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.ignore.IgnoredTypesBuilder;
import io.opentelemetry.javaagent.extension.ignore.IgnoredTypesConfigurer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

@AutoService(IgnoredTypesConfigurer.class)
public class SolarwindsIgnoredTypesConfigurer implements IgnoredTypesConfigurer {
  @Override
  public void configure(IgnoredTypesBuilder builder, ConfigProperties config) {
    builder.ignoreClass("com.solarwinds.ext.");
    builder.ignoreClass("com.solarwinds.joboe.");
  }
}
