package com.solarwinds.opentelemetry.extensions.config.provider;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ContextPropagatorComponentProviderTest {

  private final ContextPropagatorComponentProvider tested =
      new ContextPropagatorComponentProvider();

  @Test
  void testName() {
    assertEquals("swo/contextPropagator", tested.getName());
  }
}
