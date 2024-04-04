package com.solarwinds.opentelemetry.instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SwoStatementInstrumentationTest {
  @InjectMocks private SwoStatementInstrumentation tested;

  @Test
  void returnNoneMatcherWhenSqlTagIsNotEnabled() {
    ElementMatcher<TypeDescription> actual = tested.typeMatcher();
    assertEquals(none(), actual);
  }

  @Test
  void returnNonNoneMatcherWhenSqlTagIsEnabled() {
    try (MockedStatic<ConfigManager> configManagerMock = mockStatic(ConfigManager.class)) {
      configManagerMock
          .when(() -> ConfigManager.getConfigOptional(eq(ConfigProperty.AGENT_SQL_TAG), eq(false)))
          .thenReturn(true);
      ElementMatcher<TypeDescription> actual = tested.typeMatcher();
      assertNotEquals(none(), actual);
    }
  }
}
