package com.solarwinds.opentelemetry.extensions.provider;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProfilingSpanProcessorComponentProviderTest {

  private final ProfilingSpanProcessorComponentProvider tested =
      new ProfilingSpanProcessorComponentProvider();

  @Test
  void testName() {
    assertEquals("swo/profilingSpanProcessor", tested.getName());
  }
}
