package com.solarwinds.opentelemetry.extensions.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SpanExportComponentProviderTest {
  private final SpanExportComponentProvider tested = new SpanExportComponentProvider();

  @Test
  void testName() {
    assertEquals("swo/spanExporter", tested.getName());
  }
}
