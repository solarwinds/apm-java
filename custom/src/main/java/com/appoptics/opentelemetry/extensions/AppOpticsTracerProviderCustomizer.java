package com.appoptics.opentelemetry.extensions;

import static com.appoptics.opentelemetry.extensions.initialize.OtelAutoConfigurationCustomizerProviderImpl.isAgentEnabled;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import java.util.function.BiFunction;

public class AppOpticsTracerProviderCustomizer
    implements BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder> {

  @Override
  public SdkTracerProviderBuilder apply(
      SdkTracerProviderBuilder tracerProvider, ConfigProperties config) {
    if (isAgentEnabled()) {
      tracerProvider
          .setSampler(new AppOpticsSampler())
          .addSpanProcessor(new AppOpticsRootSpanProcessor())
          .addSpanProcessor(new AppOpticsProfilingSpanProcessor())
          .addSpanProcessor(new AppOpticsInboundMetricsSpanProcessor());
    }

    return tracerProvider;
  }
}
