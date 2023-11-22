package com.appoptics.api.ext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SolarWindsAgentTest {

  @BeforeAll
  static void setup() {
    SolarWindsAgent.setAgentAttachedToFalse();
  }

  @Test
  void verifyThatSetTransactionNameReturnsTrueWhenNoop() {
    assertTrue(SolarWindsAgent.setTransactionName("hello world!"));
  }

  @Test
  void verifyThatWaitUntilReadyReturnsFalseWhenNoop() {
    assertFalse(SolarWindsAgent.waitUntilReady(0, TimeUnit.MILLISECONDS));
  }
}
