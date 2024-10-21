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

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import java.util.HashSet;
import java.util.Set;

public class DbConstraintChecker {

  private static final Set<String> defaultDbs = new HashSet<>();

  static {
    defaultDbs.add(Db.mysql.name());
  }

  public static boolean isDbConfigured(Db db) {
    Set<String> configuredDbs =
        new HashSet<>(
            ConfigManager.getConfigOptional(ConfigProperty.AGENT_SQL_TAG_DATABASES, defaultDbs));
    return configuredDbs.contains(db.name());
  }

  public static boolean sqlTagEnabled() {
    return ConfigManager.getConfigOptional(ConfigProperty.AGENT_SQL_TAG, false);
  }

  public static boolean preparedSqlTagEnabled() {
    return ConfigManager.getConfigOptional(ConfigProperty.AGENT_SQL_TAG_PREPARED, false);
  }

  public enum Db {
    mysql,
    postgresql
  }

  private DbConstraintChecker() {}
}
