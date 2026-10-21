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

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Publishes the SQL settings of the {@code swo_jdbc} instrumentation config to {@link
 * ConfigManager}. Values already present (e.g. from environment variables) take precedence.
 */
final class JdbcConfig {
  static final int MAX_QUERY_LENGTH_LOWER_LIMIT = 2048;
  static final int MAX_QUERY_LENGTH_UPPER_LIMIT = 128 * 1024;

  private static final Logger logger = LoggerFactory.getLogger();

  private JdbcConfig() {}

  static void apply(DeclarativeConfigProperties config) {
    set(ConfigProperty.AGENT_SQL_TAG, config.getBoolean("sql_tag"));
    set(ConfigProperty.AGENT_SQL_TAG_PREPARED, config.getBoolean("sql_tag_prepared"));
    set(ConfigProperty.AGENT_SQL_TAG_DATABASES, databases(config));
    set(ConfigProperty.AGENT_SQL_QUERY_MAX_LENGTH, maxQueryLength(config));
  }

  private static Set<String> databases(DeclarativeConfigProperties config) {
    List<String> databases = config.getScalarList("sql_tag_databases", String.class);
    if (databases == null) {
      return null;
    }
    return databases.stream().map(String::toLowerCase).collect(Collectors.toSet());
  }

  private static Integer maxQueryLength(DeclarativeConfigProperties config) {
    Integer length = config.getInt("sql_query_max_length");
    if (length != null
        && (length < MAX_QUERY_LENGTH_LOWER_LIMIT || length > MAX_QUERY_LENGTH_UPPER_LIMIT)) {
      logger.warn(
          String.format(
              "Ignoring sql_query_max_length %d: must be between %d and %d",
              length, MAX_QUERY_LENGTH_LOWER_LIMIT, MAX_QUERY_LENGTH_UPPER_LIMIT));
      return null;
    }
    return length;
  }

  private static void set(ConfigProperty property, Object value) {
    if (value == null || ConfigManager.getConfig(property) != null) {
      return;
    }

    try {
      ConfigManager.setConfig(property, value);
    } catch (InvalidConfigException e) {
      logger.warn("Failed to apply " + property + ": " + e.getMessage());
    }
  }
}
