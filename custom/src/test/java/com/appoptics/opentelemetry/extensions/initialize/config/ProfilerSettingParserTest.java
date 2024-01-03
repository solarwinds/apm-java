package com.appoptics.opentelemetry.extensions.initialize.config;

import static org.junit.jupiter.api.Assertions.*;

import com.solarwinds.joboe.core.config.InvalidConfigException;
import com.solarwinds.joboe.core.config.ProfilerSetting;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfilerSettingParserTest {

  @InjectMocks private ProfilerSettingParser tested;

  @Test
  void returnProfilerSettingGivenValidJson() throws InvalidConfigException {
    String json =
        "{"
            + "\"enabled\": \"true\","
            + "\"interval\": 45,"
            + "\"circuitBreakerDurationThreshold\": 4,"
            + "\"circuitBreakerCountThreshold\": 5,"
            + "\"excludePackages\":[\"com.cleverchuk\", \"org.qwerty\"],"
            + "}";

    Set<String> expectedExcludes = new HashSet<>();
    expectedExcludes.add("com.cleverchuk");
    expectedExcludes.add("org.qwerty");

    ProfilerSetting actual = tested.convert(json);

    assertTrue(actual.isEnabled());
    assertEquals(45, actual.getInterval());
    assertEquals(4, actual.getCircuitBreakerDurationThreshold());

    assertEquals(5, actual.getCircuitBreakerCountThreshold());
    assertEquals(expectedExcludes, actual.getExcludePackages());
  }

  @Test
  void throwInvalidConfigExceptionGivenValidJsonWithWrongType() throws InvalidConfigException {
    String json =
        "{"
            + "\"enabled\": \"true\","
            + "\"interval\": \"this fails\","
            + "\"circuitBreakerDurationThreshold\": 4,"
            + "\"circuitBreakerCountThreshold\": 5,"
            + "\"excludePackages\":[\"com.cleverchuk\", \"org.qwerty\"],"
            + "}";
    assertThrows(InvalidConfigException.class, () -> tested.convert(json));
  }

  @Test
  void throwInvalidConfigExceptionGivenValidJsonWithWrongLowInterval()
      throws InvalidConfigException {
    String json =
        "{"
            + "\"enabled\": \"true\","
            + "\"interval\": 4,"
            + "\"circuitBreakerDurationThreshold\": 4,"
            + "\"circuitBreakerCountThreshold\": 5,"
            + "\"excludePackages\":[\"com.cleverchuk\", \"org.qwerty\"],"
            + "}";
    assertThrows(InvalidConfigException.class, () -> tested.convert(json));
  }
}
