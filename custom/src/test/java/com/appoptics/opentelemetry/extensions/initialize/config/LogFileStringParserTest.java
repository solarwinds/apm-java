package com.appoptics.opentelemetry.extensions.initialize.config;

import static org.junit.jupiter.api.Assertions.*;

import com.tracelytics.joboe.config.InvalidConfigException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogFileStringParserTest {

  @InjectMocks private LogFileStringParser tested;

  @Test
  void returnPathGivenValidPathString() throws InvalidConfigException {
    Path path = tested.convert("/chubi/path");
    assertNotNull(path);
  }

  @Test
  void throwExceptionGivenInValidPathString() {
    assertThrows(InvalidConfigException.class, () -> tested.convert("chubi/path\u0000"));
  }
}
