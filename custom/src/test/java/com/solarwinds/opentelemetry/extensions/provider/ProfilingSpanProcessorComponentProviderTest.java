package com.solarwinds.opentelemetry.extensions.provider;

import static org.junit.jupiter.api.Assertions.*;

import com.solarwinds.opentelemetry.extensions.config.provider.ProfilingSpanProcessorComponentProvider;
import org.junit.jupiter.api.Test;

class ProfilingSpanProcessorComponentProviderTest {

  private final ProfilingSpanProcessorComponentProvider tested =
      new ProfilingSpanProcessorComponentProvider();

  @Test
  void testName() {
    assertEquals("swo/profilingSpanProcessor", tested.getName());
  }
}
