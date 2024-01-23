package com.appoptics.api.ext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SolarwindsAgentTest {

  @BeforeAll
  static void setup() {
    SolarwindsAgent.setAgentAttachedToFalse();
  }

  @Test
  void verifyThatSetTransactionNameReturnsTrueWhenNoop() {
    assertTrue(SolarwindsAgent.setTransactionName("hello world!"));
  }

  @Test
  void verifyThatWaitUntilReadyReturnsFalseWhenNoop() {
    assertFalse(SolarwindsAgent.waitUntilReady(0, TimeUnit.MILLISECONDS));
  }
}
