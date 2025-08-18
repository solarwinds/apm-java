package com.solarwinds.opentelemetry.extensions.config.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HostIdResourceComponentProviderTest {
  private final HostIdResourceComponentProvider tested = new HostIdResourceComponentProvider();

  @Test
  void testName() {
    assertEquals("swo/hostIdResource", tested.getName());
  }
}
