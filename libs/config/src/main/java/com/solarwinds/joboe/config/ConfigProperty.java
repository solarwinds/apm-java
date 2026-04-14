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

package com.solarwinds.joboe.config;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * Lists and describes the properties used in configurations with its value type (Java Class Type).
 *
 * <p>It also defines the corresponding keys of the properties when used as Agent Arguments (via
 * -javaagent) or in configuration file.
 *
 * <p>Lookups are provided in order to retrieve the <code>ConfigProperty</code> from Agent Argument
 * and from Key (property file key)
 *
 * @author Patson Luk
 */
public enum ConfigProperty {
  AGENT_CONFIG(
      new ConfigKey(null, EnvPrefix.PRODUCT + "CONFIG_FILE"), ConfigGroup.AGENT, String.class),
  AGENT_EXPORT_LOGS_ENABLED(
      new ConfigKey("agent.exportLogsEnabled", EnvPrefix.PRODUCT + "EXPORT_LOGS_ENABLED"),
      ConfigGroup.AGENT,
      Boolean.class),
  AGENT_EXPORT_METRICS_ENABLED(
      new ConfigKey("agent.exportMetricsEnabled", EnvPrefix.PRODUCT + "EXPORT_METRICS_ENABLED"),
      ConfigGroup.AGENT,
      Boolean.class),
  AGENT_SPAN_STACKTRACE_FILTERS(
      new ConfigKey("agent.spanStacktraceFilters", EnvPrefix.PRODUCT + "SPAN_STACKTRACE_FILTERS"),
      ConfigGroup.AGENT,
      String.class),
  AGENT_TRANSACTION_NAME(
      new ConfigKey(null, EnvPrefix.PRODUCT + "TRANSACTION_NAME"), ConfigGroup.AGENT, String.class),
  AGENT_CONFIG_FILE_WATCH_PERIOD(
      new ConfigKey("agent.configFileWatchPeriod"), ConfigGroup.AGENT, Long.class),
  AGENT_EVENTS_SEND_CAPACITY(
      new ConfigKey(null, EnvPrefix.PRODUCT + "EVENTS_SEND_CAPACITY"),
      ConfigGroup.AGENT,
      Integer.class),
  AGENT_DEBUG(new ConfigKey(null, null), ConfigGroup.AGENT, Boolean.class),
  AGENT_LOGGING(
      new ConfigKey("agent.logging", EnvPrefix.PRODUCT + "DEBUG_LEVEL"),
      ConfigGroup.AGENT,
      String.class),
  AGENT_TRACING_MODE(new ConfigKey("agent.tracingMode", null), ConfigGroup.AGENT, String.class),
  AGENT_SERVICE_KEY(
      new ConfigKey("agent.serviceKey", EnvPrefix.PRODUCT + "SERVICE_KEY"),
      ConfigGroup.AGENT,
      String.class),
  AGENT_SQL_QUERY_MAX_LENGTH(
      new ConfigKey("agent.sqlQueryMaxLength", EnvPrefix.PRODUCT + "MAX_SQL_QUERY_LENGTH"),
      ConfigGroup.AGENT,
      Integer.class),
  AGENT_URL_SAMPLE_RATE(new ConfigKey("agent.urlSampleRates"), ConfigGroup.AGENT, String.class),
  AGENT_TIME_ADJUST_INTERVAL(
      new ConfigKey("agent.timeAdjustInterval"), ConfigGroup.AGENT, Integer.class),
  AGENT_CONTEXT_TTL(new ConfigKey("agent.contextTtl"), ConfigGroup.AGENT, Integer.class),
  AGENT_CONTEXT_MAX_EVENTS(
      new ConfigKey("agent.contextMaxEvents"), ConfigGroup.AGENT, Integer.class),
  AGENT_CONTEXT_MAX_BACKTRACES(
      new ConfigKey("agent.contextMaxBacktraces"), ConfigGroup.AGENT, Integer.class),
  AGENT_HOSTNAME_ALIAS(
      new ConfigKey("agent.hostnameAlias", EnvPrefix.PRODUCT + "HOSTNAME_ALIAS"),
      ConfigGroup.AGENT,
      String.class),
  AGENT_LOG_FILE(
      new ConfigKey("agent.javaLogFile", EnvPrefix.PRODUCT + "JAVA_LOG_FILE"),
      ConfigGroup.AGENT,
      String.class),
  AGENT_COLLECTOR(
      new ConfigKey("agent.collector", EnvPrefix.PRODUCT + "COLLECTOR"),
      ConfigGroup.AGENT,
      String.class),
  AGENT_COLLECTOR_SERVER_CERT_LOCATION(
      new ConfigKey(null, EnvPrefix.PRODUCT + "TRUSTEDPATH"), ConfigGroup.AGENT, String.class),
  AGENT_EVENTS_FLUSH_INTERVAL(
      new ConfigKey(null, EnvPrefix.PRODUCT + "EVENTS_FLUSH_INTERVAL"),
      ConfigGroup.AGENT,
      Integer.class),
  AGENT_TRANSACTION_NAME_PATTERN(
      new ConfigKey("transaction.namePattern"), ConfigGroup.AGENT, String.class),
  AGENT_DOMAIN_PREFIXED_TRANSACTION_NAME(
      new ConfigKey("transaction.prependDomain"), ConfigGroup.AGENT, Boolean.class),
  AGENT_TRANSACTION_SETTINGS(
      new ConfigKey("agent.transactionSettings"), ConfigGroup.AGENT, String.class),
  AGENT_TRANSACTION_NAMING_SCHEMES(
      new ConfigKey("agent.transactionNameSchemes"), ConfigGroup.AGENT, String.class),
  // AGENT_INTERNAL_TRANSACTION_SETTINGS should NOT be specified directly in the json file. This is
  // used to store the result after comparing AGENT_TRANSACTION_SETTINGS and AGENT_URL_SAMPLE_RATE -
  // not an ideal solution as this is confusing
  AGENT_INTERNAL_TRANSACTION_SETTINGS(
      new ConfigKey("agent.internal.transactionSettings"), ConfigGroup.AGENT, String.class),
  AGENT_EC2_METADATA_TIMEOUT(
      new ConfigKey("agent.ec2MetadataTimeout", EnvPrefix.PRODUCT + "EC2_METADATA_TIMEOUT"),
      ConfigGroup.AGENT,
      Integer.class),
  AGENT_AZURE_VM_METADATA_TIMEOUT(
      new ConfigKey(
          "agent.azureVmMetadataTimeout", EnvPrefix.PRODUCT + "AZURE_VM_METADATA_TIMEOUT"),
      ConfigGroup.AGENT,
      Integer.class),
  AGENT_AZURE_VM_METADATA_VERSION(
      new ConfigKey(
          "agent.azureVmMetadataVersion", EnvPrefix.PRODUCT + "AZURE_VM_METADATA_VERSION"),
      ConfigGroup.AGENT,
      String.class),
  AGENT_COLLECTOR_TIMEOUT(
      new ConfigKey("agent.collectorTimeout", EnvPrefix.PRODUCT + "COLLECTOR_TIMEOUT"),
      ConfigGroup.AGENT,
      Integer.class),
  AGENT_TRIGGER_TRACE_ENABLED(
      new ConfigKey("agent.triggerTrace", EnvPrefix.PRODUCT + "TRIGGER_TRACE"),
      ConfigGroup.AGENT,
      String.class),
  AGENT_PROXY(
      new ConfigKey("agent.proxy", EnvPrefix.PRODUCT + "PROXY"), ConfigGroup.AGENT, String.class),
  AGENT_GRPC_COMPRESSION(
      new ConfigKey(null, EnvPrefix.PRODUCT + "GRPC_COMPRESSION"),
      ConfigGroup.AGENT,
      String.class), // not advertised
  AGENT_SQL_TAG(
      new ConfigKey("agent.sqlTag", EnvPrefix.PRODUCT + "SQL_TAG"),
      ConfigGroup.AGENT,
      Boolean.class),
  AGENT_SQL_TAG_PREPARED(
      new ConfigKey("agent.sqlTagPrepared", EnvPrefix.PRODUCT + "SQL_TAG_PREPARED"),
      ConfigGroup.AGENT,
      Boolean.class),
  AGENT_SQL_TAG_DATABASES(
      new ConfigKey("agent.sqlTagDatabases", EnvPrefix.PRODUCT + "SQL_TAG_DATABASES"),
      ConfigGroup.AGENT,
      String.class),
  PROFILER(new ConfigKey("profiler"), ConfigGroup.PROFILER, String.class),
  PROFILER_ENABLED_ENV_VAR(
      new ConfigKey(null, EnvPrefix.PRODUCT + "PROFILER_ENABLED"),
      ConfigGroup.PROFILER,
      Boolean.class),
  PROFILER_INTERVAL_ENV_VAR(
      new ConfigKey(null, EnvPrefix.PRODUCT + "PROFILER_INTERVAL"),
      ConfigGroup.PROFILER,
      Integer.class);
  private final ConfigKey configKey;

  /**
   * -- GETTER --
   *
   * @return the Java Class of the property value
   */
  @Getter private final Class<? extends Serializable> typeClass;

  private final ConfigGroup group;
  @Getter private ConfigParser<?, ?> configParser;

  public static class EnvPrefix {
    public static final String PRODUCT = "SW_APM_";
  }

  private static class ConfigKey {
    private final String configFileKey;
    private final String environmentVariableKey;

    public ConfigKey(String configFileKey) {
      this(configFileKey, null);
    }

    public ConfigKey(String configFileKey, String environmentVariableKey) {
      super();
      this.configFileKey = configFileKey;
      this.environmentVariableKey = environmentVariableKey;
    }
  }

  /**
   * @param configKey keys used to map to this property
   * @param group
   * @param typeClass the Java Class of the property value
   */
  ConfigProperty(ConfigKey configKey, ConfigGroup group, Class<? extends Serializable> typeClass) {
    this.configKey = configKey;
    this.typeClass = typeClass;
    this.group = group;
    registerLookup(this);
  }

  public void setParser(ConfigParser<?, ?> configParser) {
    this.configParser = configParser;
  }

  private static void registerLookup(ConfigProperty property) {
    String configFileKey = property.configKey.configFileKey;
    if (configFileKey != null) {
      // put the key into the lookup map
      ConfigPropertyRegistry.CONFIG_FILE_KEY_TO_PARAMETER.put(configFileKey, property);
    }

    String environmentVariableKey = property.configKey.environmentVariableKey;
    if (environmentVariableKey != null) {
      // put the key into the lookup map
      ConfigPropertyRegistry.ENVIRONMENT_VARIABLE_KEY_TO_PARAMETER.put(
          environmentVariableKey, property);
    }
  }

  /**
   * @param key the key used in configuration property file
   * @return the corresponding ConfigProperty by the key. Null if the property is not defined under
   *     that key
   */
  public static ConfigProperty fromConfigFileKey(String key) {
    return ConfigPropertyRegistry.CONFIG_FILE_KEY_TO_PARAMETER.get(key);
  }

  public String getConfigFileKey() {
    return configKey.configFileKey;
  }

  public String getEnvironmentVariableKey() {
    return configKey.environmentVariableKey;
  }

  /**
   * @param environmentVariableKey the environment variable key used
   * @return the corresponding ConfigProperty by the environment variable key used. Null if the
   *     property is not defined under that environment variable key
   */
  public static ConfigProperty fromEnvironmentVariableKey(String environmentVariableKey) {
    return ConfigPropertyRegistry.ENVIRONMENT_VARIABLE_KEY_TO_PARAMETER.get(environmentVariableKey);
  }

  public static Map<String, ConfigProperty> getEnviromentVariableMap() {
    return ConfigPropertyRegistry.ENVIRONMENT_VARIABLE_KEY_TO_PARAMETER;
  }

  /**
   * @return the Grouping of this ConfigProperty
   */
  ConfigGroup getGroup() {
    return group;
  }

  static class ConfigPropertyRegistry {
    private static final Map<String, ConfigProperty> CONFIG_FILE_KEY_TO_PARAMETER =
        new HashMap<String, ConfigProperty>();
    private static final Map<String, ConfigProperty> ENVIRONMENT_VARIABLE_KEY_TO_PARAMETER =
        new HashMap<String, ConfigProperty>();
  }
}
