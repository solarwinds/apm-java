package com.solarwinds.opentelemetry.extensions.config.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MetricExporterComponentProviderTest {
  private final MetricExporterComponentProvider tested = new MetricExporterComponentProvider();

  @Test
  void testName() {
    assertEquals("swo/metricExporter", tested.getName());
  }
}
