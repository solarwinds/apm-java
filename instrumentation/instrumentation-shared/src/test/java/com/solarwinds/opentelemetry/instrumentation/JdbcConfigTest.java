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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class JdbcConfigTest {
  @BeforeEach
  @AfterEach
  void reset() {
    ConfigManager.reset();
  }

  @Test
  void publishesAllSqlSettings() {
    DeclarativeConfigProperties config = Mockito.mock(DeclarativeConfigProperties.class);
    when(config.getBoolean("sql_tag")).thenReturn(true);
    when(config.getBoolean("sql_tag_prepared")).thenReturn(true);

    when(config.getScalarList("sql_tag_databases", String.class))
        .thenReturn(Arrays.asList("PostgreSQL", "mysql"));
    when(config.getInt("sql_query_max_length")).thenReturn(4096);

    JdbcConfig.apply(config);

    assertEquals(true, ConfigManager.getConfig(ConfigProperty.AGENT_SQL_TAG));
    assertEquals(true, ConfigManager.getConfig(ConfigProperty.AGENT_SQL_TAG_PREPARED));

    assertEquals(
        new HashSet<>(Arrays.asList("postgresql", "mysql")),
        ConfigManager.getConfig(ConfigProperty.AGENT_SQL_TAG_DATABASES));
    assertEquals(4096, ConfigManager.getConfig(ConfigProperty.AGENT_SQL_QUERY_MAX_LENGTH));
  }

  @Test
  void leavesSettingsUnsetWhenNodeIsEmpty() {
    JdbcConfig.apply(DeclarativeConfigProperties.empty());

    assertNull(ConfigManager.getConfig(ConfigProperty.AGENT_SQL_TAG));
    assertNull(ConfigManager.getConfig(ConfigProperty.AGENT_SQL_TAG_DATABASES));
    assertNull(ConfigManager.getConfig(ConfigProperty.AGENT_SQL_QUERY_MAX_LENGTH));
  }

  @Test
  void ignoresOutOfRangeMaxQueryLength() {
    DeclarativeConfigProperties config = Mockito.mock(DeclarativeConfigProperties.class);
    when(config.getInt("sql_query_max_length")).thenReturn(10);

    JdbcConfig.apply(config);
    assertNull(ConfigManager.getConfig(ConfigProperty.AGENT_SQL_QUERY_MAX_LENGTH));
  }

  @Test
  void doesNotOverrideExistingSettings() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_SQL_TAG_DATABASES, Collections.singleton("mysql"));
    DeclarativeConfigProperties config = Mockito.mock(DeclarativeConfigProperties.class);
    when(config.getScalarList("sql_tag_databases", String.class))
        .thenReturn(Collections.singletonList("postgresql"));

    JdbcConfig.apply(config);

    assertEquals(
        Collections.singleton("mysql"),
        ConfigManager.getConfig(ConfigProperty.AGENT_SQL_TAG_DATABASES));
  }
}
