package com.solarwinds.opentelemetry.extensions.config.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SamplerComponentProviderTest {
  private final SamplerComponentProvider tested = new SamplerComponentProvider();

  @Test
  void testName() {
    assertEquals("swo/sampler", tested.getName());
  }
}
