package com.solarwinds.opentelemetry.extensions.lambda;

import static com.solarwinds.opentelemetry.extensions.initialize.AutoConfigurationCustomizerProviderImpl.isAgentEnabled;
import static com.solarwinds.joboe.core.util.HostTypeDetector.isLambda;

import com.solarwinds.opentelemetry.extensions.SolarwindsRootSpanProcessor;
import com.solarwinds.opentelemetry.extensions.SolarwindsSampler;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import java.util.function.BiFunction;

public class RuntimeTraceProviderCustomizer
    implements BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder> {
  private final BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder>
      delegate;

  public RuntimeTraceProviderCustomizer(
      BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder> delegate) {
    this.delegate = delegate;
  }

  @Override
  public SdkTracerProviderBuilder apply(
      SdkTracerProviderBuilder sdkTracerProviderBuilder, ConfigProperties configProperties) {
    if (isAgentEnabled()) {
      if (isLambda()) {
        sdkTracerProviderBuilder
            .setSampler(new SolarwindsSampler())
            .addSpanProcessor(new SolarwindsRootSpanProcessor())
            .addSpanProcessor(new InboundMeasurementMetricsGenerator());
      } else {
        return delegate.apply(sdkTracerProviderBuilder, configProperties);
      }
    }

    return sdkTracerProviderBuilder;
  }
}
