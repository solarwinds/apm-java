package com.solarwinds.opentelemetry.extensions.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.semconv.incubating.HostIncubatingAttributes;
import io.opentelemetry.semconv.incubating.ProcessIncubatingAttributes;
import org.junit.jupiter.api.Test;

class HostIdResourceUtilTest {
  @Test
  void testCreateAttributes() {
    Attributes attribute = HostIdResourceUtil.createAttribute();
    Long pid = attribute.get(ProcessIncubatingAttributes.PROCESS_PID);
    String hostname = attribute.get(HostIncubatingAttributes.HOST_NAME);

    assertNotNull(pid);
    assertNotNull(hostname);
  }
}
