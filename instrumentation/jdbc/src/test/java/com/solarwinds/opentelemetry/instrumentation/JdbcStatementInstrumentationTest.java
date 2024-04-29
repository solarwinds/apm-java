package com.solarwinds.opentelemetry.instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JdbcStatementInstrumentationTest {
  @InjectMocks private JdbcStatementInstrumentation tested;

  @Test
  void returnNoneMatcherWhenSqlTagIsNotEnabled() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_SQL_TAG, false);
    ElementMatcher<TypeDescription> actual = tested.typeMatcher();
    assertEquals(none(), actual);
  }

  @Test
  void returnNonNoneMatcherWhenSqlTagIsEnabled() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_SQL_TAG, true);
    ElementMatcher<TypeDescription> actual = tested.typeMatcher();
    assertNotEquals(none(), actual);
  }
}
