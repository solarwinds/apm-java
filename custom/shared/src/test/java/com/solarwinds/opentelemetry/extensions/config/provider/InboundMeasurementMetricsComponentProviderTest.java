package com.solarwinds.opentelemetry.extensions.config.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InboundMeasurementMetricsComponentProviderTest {
  private final InboundMeasurementMetricsComponentProvider tested =
      new InboundMeasurementMetricsComponentProvider();

  @Test
  void testName() {
    assertEquals("swo/inboundMeasurementMetrics", tested.getName());
  }
}
