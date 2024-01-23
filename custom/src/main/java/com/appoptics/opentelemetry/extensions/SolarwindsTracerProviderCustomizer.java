package com.appoptics.opentelemetry.extensions;

import static com.appoptics.opentelemetry.extensions.initialize.OtelAutoConfigurationCustomizerProviderImpl.isAgentEnabled;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import java.util.function.BiFunction;

public class SolarwindsTracerProviderCustomizer
    implements BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder> {

  @Override
  public SdkTracerProviderBuilder apply(
      SdkTracerProviderBuilder tracerProvider, ConfigProperties config) {
    if (isAgentEnabled()) {
      tracerProvider
          .setSampler(new SolarwindsSampler())
          .addSpanProcessor(new SolarwindsRootSpanProcessor())
          .addSpanProcessor(new SolarwindsProfilingSpanProcessor())
          .addSpanProcessor(new SolarwindsInboundMetricsSpanProcessor());
    }

    return tracerProvider;
  }
}