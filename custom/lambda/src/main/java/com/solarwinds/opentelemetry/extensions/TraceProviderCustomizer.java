package com.solarwinds.opentelemetry.extensions;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import java.util.function.BiFunction;

public class TraceProviderCustomizer
    implements BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder> {

  @Override
  public SdkTracerProviderBuilder apply(
      SdkTracerProviderBuilder sdkTracerProviderBuilder, ConfigProperties configProperties) {
    return sdkTracerProviderBuilder
        .setSampler(new SolarwindsSampler())
        .addSpanProcessor(new InboundMeasurementMetricsGenerator());
  }
}
