/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
class JdbcPreparedStatementInstrumentationTest {
  @InjectMocks private JdbcPreparedStatementInstrumentation tested;

  @Test
  void returnNoneMatcherWhenSqlTagPreparedIsNotEnabled() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_SQL_TAG_PREPARED, false);
    ElementMatcher<TypeDescription> actual = tested.typeMatcher();
    assertEquals(none(), actual);
  }

  @Test
  void returnNonNoneMatcherWhenSqlTagPreparedIsEnabled() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_SQL_TAG_PREPARED, true);
    ElementMatcher<TypeDescription> actual = tested.typeMatcher();
    assertNotEquals(none(), actual);
  }
}
