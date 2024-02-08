package com.solarwinds.opentelemetry.extensions.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.solarwinds.joboe.core.config.ConfigManager;
import com.solarwinds.joboe.core.config.ConfigProperty;
import com.solarwinds.joboe.core.config.InvalidConfigException;
import com.solarwinds.opentelemetry.extensions.transaction.DefaultNamingScheme;
import com.solarwinds.opentelemetry.extensions.transaction.NamingScheme;
import io.opentelemetry.api.common.Attributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultNamingSchemeTest {

  @InjectMocks private DefaultNamingScheme tested;

  @Mock private NamingScheme namingSchemeMock;

  @Test
  void verifyNoDelegationToNext() {
    String name = tested.createName(Attributes.empty());

    verify(namingSchemeMock, times(0)).createName(any());
    assertNull(name);
  }

  @Test
  void verifyTransactionNameIsReturnedWhenSetInEnvironment() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_TRANSACTION_NAME, "test");
    String name = tested.createName(Attributes.empty());

    assertEquals("test", name);
  }
}
