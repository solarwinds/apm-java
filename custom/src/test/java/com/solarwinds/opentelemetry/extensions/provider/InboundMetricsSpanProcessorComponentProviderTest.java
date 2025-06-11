package com.solarwinds.opentelemetry.extensions.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.solarwinds.opentelemetry.extensions.config.provider.InboundMetricsSpanProcessorComponentProvider;
import org.junit.jupiter.api.Test;

class InboundMetricsSpanProcessorComponentProviderTest {

  private final InboundMetricsSpanProcessorComponentProvider tested =
      new InboundMetricsSpanProcessorComponentProvider();

  @Test
  void testName() {
    assertEquals("swo/inboundMetricSpanProcessor", tested.getName());
  }
}
