package com.solarwinds.opentelemetry.extensions.initialize.config;

import static org.junit.jupiter.api.Assertions.*;

import com.solarwinds.joboe.core.config.InvalidConfigException;
import com.solarwinds.opentelemetry.extensions.initialize.config.ModeStringToBooleanParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModeStringToBooleanParserTest {

  @InjectMocks private ModeStringToBooleanParser tested;

  @Test
  void returnTrueGivenEnabled() throws InvalidConfigException {
    assertTrue(tested.convert("enabled"));
  }

  @Test
  void returnFalseGivenDisabled() throws InvalidConfigException {
    assertFalse(tested.convert("disabled"));
  }

  @Test
  void throwInvalidConfigExceptionGivenInvalidInput() throws InvalidConfigException {
    assertThrows(InvalidConfigException.class, () -> tested.convert("miss me? NO"));
  }
}
