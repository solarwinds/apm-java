package com.solarwinds.opentelemetry.extensions;

import static com.solarwinds.opentelemetry.extensions.initialize.AutoConfigurationCustomizerProviderImpl.isAgentEnabled;

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
          .addSpanProcessor(new SolarwindsProfilingSpanProcessor())
          .addSpanProcessor(new SolarwindsInboundMetricsSpanProcessor());
    }

    return tracerProvider;
  }
}
