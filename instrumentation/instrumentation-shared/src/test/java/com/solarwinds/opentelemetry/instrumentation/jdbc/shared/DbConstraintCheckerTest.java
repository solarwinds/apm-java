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

package com.solarwinds.opentelemetry.instrumentation.jdbc.shared;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DbConstraintCheckerTest {

  private static final Set<String> activeDbs = new HashSet<>();

  @BeforeAll
  static void setup() {
    activeDbs.add("postgresql");
  }

  @Test
  void returnTrueWhenDbIsNotConfiguredAndInputIsMysql() {
    ConfigManager.removeConfig(ConfigProperty.AGENT_SQL_TAG_DATABASES);
    assertTrue(DbConstraintChecker.isDbConfigured(DbConstraintChecker.Db.mysql));
  }

  @Test
  void returnTrueWhenDbIsConfiguredAndInputIsPostgresql() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_SQL_TAG_DATABASES, activeDbs);
    assertTrue(DbConstraintChecker.isDbConfigured(DbConstraintChecker.Db.postgresql));
  }

  @Test
  void returnFalseWhenDbIsNotConfigured() {
    ConfigManager.removeConfig(ConfigProperty.AGENT_SQL_TAG_DATABASES);
    assertFalse(DbConstraintChecker.isDbConfigured(DbConstraintChecker.Db.postgresql));
  }
}
