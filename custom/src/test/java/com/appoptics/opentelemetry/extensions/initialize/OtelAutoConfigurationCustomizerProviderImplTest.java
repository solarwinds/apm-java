package com.appoptics.opentelemetry.extensions.initialize;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class OtelAutoConfigurationCustomizerProviderImplTest {

  @Test
  void verifyThatWhenDisabledItIsNeverEnabled() {
    OtelAutoConfigurationCustomizerProviderImpl.setAgentEnabled(false);
    OtelAutoConfigurationCustomizerProviderImpl.setAgentEnabled(true);
    assertFalse(OtelAutoConfigurationCustomizerProviderImpl.isAgentEnabled());
  }
}