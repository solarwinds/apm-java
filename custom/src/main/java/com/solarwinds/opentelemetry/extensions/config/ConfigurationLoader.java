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

package com.solarwinds.opentelemetry.extensions.config;

import com.solarwinds.joboe.config.ConfigContainer;
import com.solarwinds.joboe.config.ConfigGroup;
import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.ConfigSourceType;
import com.solarwinds.joboe.config.EnvConfigReader;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.config.InvalidConfigReadSourceException;
import com.solarwinds.joboe.config.InvalidConfigServiceKeyException;
import com.solarwinds.joboe.config.JsonConfigReader;
import com.solarwinds.joboe.config.ServiceKeyUtils;
import com.solarwinds.joboe.core.profiler.ProfilerSetting;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.TraceConfigs;
import com.solarwinds.opentelemetry.extensions.Constants;
import com.solarwinds.opentelemetry.extensions.LoggingConfigProvider;
import com.solarwinds.opentelemetry.extensions.NamingScheme;
import com.solarwinds.opentelemetry.extensions.TransactionNameManager;
import com.solarwinds.opentelemetry.extensions.TransactionNamingScheme;
import com.solarwinds.opentelemetry.extensions.config.livereload.ConfigurationFileWatcher;
import com.solarwinds.opentelemetry.extensions.config.parser.json.LogFileStringParser;
import com.solarwinds.opentelemetry.extensions.config.parser.json.LogSettingParser;
import com.solarwinds.opentelemetry.extensions.config.parser.json.ModeStringToBooleanParser;
import com.solarwinds.opentelemetry.extensions.config.parser.json.ProfilerSettingParser;
import com.solarwinds.opentelemetry.extensions.config.parser.json.ProxyConfigParser;
import com.solarwinds.opentelemetry.extensions.config.parser.json.RangeValidationParser;
import com.solarwinds.opentelemetry.extensions.config.parser.json.SqlTagDatabasesParser;
import com.solarwinds.opentelemetry.extensions.config.parser.json.StacktraceFilterParser;
import com.solarwinds.opentelemetry.extensions.config.parser.json.TracingModeParser;
import com.solarwinds.opentelemetry.extensions.config.parser.json.TransactionNamingSchemesParser;
import com.solarwinds.opentelemetry.extensions.config.parser.json.TransactionSettingsConfigParser;
import com.solarwinds.opentelemetry.extensions.config.parser.json.UrlSampleRateConfigParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import lombok.Getter;

public class ConfigurationLoader {
  private static final Logger logger = LoggerFactory.getLogger();
  private static final String CONFIG_FILE = "solarwinds-apm-config.json";
  private static final String SYS_PROPERTIES_PREFIX = "sw.apm";

  private static final String PROTOCOL = "http/protobuf";

  @Getter private static String configurationFileDir = null;

  @Getter private static String runtimeConfigFilename = null;

  private static WatchService watchService = null;

  private static ScheduledExecutorService watchScheduler;

  static {
    ConfigProperty.AGENT_LOGGING.setParser(LogSettingParser.INSTANCE);
    ConfigProperty.AGENT_LOG_FILE.setParser(new LogFileStringParser());
    ConfigProperty.AGENT_TRACING_MODE.setParser(new TracingModeParser());
    ConfigProperty.AGENT_SQL_QUERY_MAX_LENGTH.setParser(
        new RangeValidationParser<Integer>(
            Constants.MAX_SQL_QUERY_LENGTH_LOWER_LIMIT,
            Constants.MAX_SQL_QUERY_LENGTH_UPPER_LIMIT));
    ConfigProperty.AGENT_URL_SAMPLE_RATE.setParser(UrlSampleRateConfigParser.INSTANCE);
    ConfigProperty.AGENT_TRANSACTION_SETTINGS.setParser(TransactionSettingsConfigParser.INSTANCE);
    ConfigProperty.AGENT_TRIGGER_TRACE_ENABLED.setParser(ModeStringToBooleanParser.INSTANCE);
    ConfigProperty.AGENT_PROXY.setParser(ProxyConfigParser.INSTANCE);
    ConfigProperty.PROFILER.setParser(ProfilerSettingParser.INSTANCE);
    ConfigProperty.AGENT_TRANSACTION_NAMING_SCHEMES.setParser(new TransactionNamingSchemesParser());
    ConfigProperty.AGENT_SQL_TAG_DATABASES.setParser(new SqlTagDatabasesParser());
    ConfigProperty.AGENT_SPAN_STACKTRACE_FILTERS.setParser(new StacktraceFilterParser());
  }

  public static void load() throws InvalidConfigException {
    if (isDeclarativeConfigOff()) {
      loadConfigurations();
      String serviceKey = (String) ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY);
      logger.info(
          "Successfully loaded SolarwindsAPM OpenTelemetry extensions configurations. Service key: "
              + ServiceKeyUtils.maskServiceKey(serviceKey));
    }
  }

  private static void attachConfigurationFileWatcher() {
    Long watchPeriod =
        ConfigManager.getConfigOptional(ConfigProperty.AGENT_CONFIG_FILE_WATCH_PERIOD, 0L);
    if (watchPeriod > 0 && configurationFileDir != null && runtimeConfigFilename != null) {
      try {
        if (watchService == null) {
          watchService = FileSystems.getDefault().newWatchService();
          watchScheduler = Executors.newSingleThreadScheduledExecutor();
        }

        ConfigurationFileWatcher.restartWatch(
            Paths.get(configurationFileDir),
            watchPeriod,
            watchService,
            watchScheduler,
            () -> {
              try {
                logger.info("Configuration file change detected. Reloading configuration");
                loadConfigurations();
              } catch (InvalidConfigException invalidConfigException) {
                logger.info(
                    String.format(
                        "Invalid configuration found on reload. Error - %s",
                        invalidConfigException));
              }
            });
      } catch (Throwable e) {
        logger.warn(String.format("Failed to attach configuration file watcher due error: %s", e));
      }
    }
  }

  /**
   * Checks the OpenTelemetry Java agent's logger settings. If the NH custom distro doesn't set a
   * log file but the Otel has this config option, we just follow the Otel's config.
   *
   * @param configs the configuration container to hold configs
   */
  private static void maybeFollowOtelConfigProperties(ConfigContainer configs) {
    if (configs.get(ConfigProperty.AGENT_LOG_FILE) == null
        && System.getProperty("io.opentelemetry.javaagent.slf4j.simpleLogger.logFile") != null) {
      try {
        Path path =
            Paths.get(System.getProperty("io.opentelemetry.javaagent.slf4j.simpleLogger.logFile"));
        configs.put(ConfigProperty.AGENT_LOG_FILE, path);
      } catch (InvalidConfigException | InvalidPathException e) {
        logger.info("failed to follow Otel's log file config." + e.getMessage());
      }
    }

    String serviceName = System.getProperty("otel.service.name");
    if (serviceName == null) {
      serviceName = System.getenv("OTEL_SERVICE_NAME");
    }

    String serviceKey = (String) configs.get(ConfigProperty.AGENT_SERVICE_KEY);
    if (serviceName == null) {
      if (serviceKey != null) {
        String name = ServiceKeyUtils.getServiceName(serviceKey);
        if (name != null) {
          setSystemProperty("otel.service.name", name);
        }
      }

    } else {
      if (serviceKey != null) {
        try {
          String key = String.format("%s:%s", ServiceKeyUtils.getApiKey(serviceKey), serviceName);
          configs.put(ConfigProperty.AGENT_SERVICE_KEY, key, true);
        } catch (InvalidConfigException e) {
          LoggerFactory.getLogger()
              .warn(String.format("Unable to update service name to %s", serviceName));
        }
      }
    }
  }

  static void configureOtelLogExport(ConfigContainer container) throws InvalidConfigException {
    Boolean exportLog = (Boolean) container.get(ConfigProperty.AGENT_EXPORT_LOGS_ENABLED);
    if (exportLog != null && exportLog) {
      String serviceKey = (String) container.get(ConfigProperty.AGENT_SERVICE_KEY);
      String apiKey = ServiceKeyUtils.getApiKey(serviceKey);

      String dataCell = "na-01";
      String env = "cloud";
      String collectorEndpoint = (String) container.get(ConfigProperty.AGENT_COLLECTOR);

      if (collectorEndpoint != null) {
        if (collectorEndpoint.contains("appoptics.com")) {
          throw new InvalidConfigException("Appoptics is not supported");
        }
        collectorEndpoint = collectorEndpoint.split(":")[0];
        String[] fragments = collectorEndpoint.split("\\.");
        if (fragments.length > 2) {
          // This is based on knowledge of the SWO url format where the third name from the left in
          // the domain is the data-cell name and assumes this format will stay stable.
          dataCell = fragments[2];
        }

        if (fragments.length > 3) {
          env = fragments[3];
        }
      }

      String protocol = setProtocol("otel.exporter.otlp.logs.protocol");
      setSystemProperty("otel.logs.exporter", "otlp");
      setSystemProperty(
          "otel.exporter.otlp.logs.headers", String.format("authorization=Bearer %s", apiKey));

      if (protocol.equalsIgnoreCase("grpc")) {
        setEndpoint(
            "otel.exporter.otlp.logs.endpoint",
            String.format("https://otel.collector.%s.%s.solarwinds.com", dataCell, env),
            "");
        return;
      }

      setEndpoint(
          "otel.exporter.otlp.logs.endpoint",
          String.format("https://otel.collector.%s.%s.solarwinds.com/v1/logs", dataCell, env),
          "/v1/logs");
    }
  }

  static void configureOtelMetricExport(ConfigContainer container) throws InvalidConfigException {
    String serviceKey = (String) container.get(ConfigProperty.AGENT_SERVICE_KEY);
    String apiKey = ServiceKeyUtils.getApiKey(serviceKey);
    String dataCell = "na-01";

    String env = "cloud";
    String collectorEndpoint = (String) container.get(ConfigProperty.AGENT_COLLECTOR);
    if (collectorEndpoint != null) {
      if (collectorEndpoint.contains("appoptics.com")) {
        throw new InvalidConfigException("Appoptics is not supported");
      }

      collectorEndpoint = collectorEndpoint.split(":")[0];
      String[] fragments = collectorEndpoint.split("\\.");
      if (fragments.length > 2) {
        // This is based on knowledge of the SWO url format where the third name from the left in
        // the domain is the data-cell name and assumes this format will stay stable.
        dataCell = fragments[2];
      }

      if (fragments.length > 3) {
        env = fragments[3];
      }
    }

    String protocol = setProtocol("otel.exporter.otlp.metrics.protocol");
    setSystemProperty("otel.metrics.exporter", "otlp");
    setSystemProperty(
        "otel.exporter.otlp.metrics.headers", String.format("authorization=Bearer %s", apiKey));

    if (protocol.equalsIgnoreCase("grpc")) {
      setEndpoint(
          "otel.exporter.otlp.metrics.endpoint",
          String.format("https://otel.collector.%s.%s.solarwinds.com", dataCell, env),
          "");
      return;
    }

    setEndpoint(
        "otel.exporter.otlp.metrics.endpoint",
        String.format("https://otel.collector.%s.%s.solarwinds.com/v1/metrics", dataCell, env),
        "/v1/metrics");
  }

  static void configureOtelTraceExport(ConfigContainer container) throws InvalidConfigException {
    String serviceKey = (String) container.get(ConfigProperty.AGENT_SERVICE_KEY);
    String apiKey = ServiceKeyUtils.getApiKey(serviceKey);

    String dataCell = "na-01";
    String env = "cloud";
    String collectorEndpoint = (String) container.get(ConfigProperty.AGENT_COLLECTOR);

    if (collectorEndpoint != null) {
      if (collectorEndpoint.contains("appoptics.com")) {
        throw new InvalidConfigException("Appoptics is not supported");
      }

      collectorEndpoint = collectorEndpoint.split(":")[0];
      String[] fragments = collectorEndpoint.split("\\.");
      if (fragments.length > 2) {
        // This is based on knowledge of the SWO url format where the third name from the left in
        // the domain is the data-cell name and assumes this format will stay stable.
        dataCell = fragments[2];
      }

      if (fragments.length > 3) {
        env = fragments[3];
      }
    }

    String protocol = setProtocol("otel.exporter.otlp.traces.protocol");
    setSystemProperty(
        "otel.exporter.otlp.traces.headers", String.format("authorization=Bearer %s", apiKey));

    if (protocol.equalsIgnoreCase("grpc")) {
      setEndpoint(
          "otel.exporter.otlp.traces.endpoint",
          String.format("https://otel.collector.%s.%s.solarwinds.com", dataCell, env),
          "");
      return;
    }

    setEndpoint(
        "otel.exporter.otlp.traces.endpoint",
        String.format("https://otel.collector.%s.%s.solarwinds.com/v1/traces", dataCell, env),
        "/v1/traces");
  }

  static Map<String, String> mergeEnvWithSysProperties(Map<String, String> env, Properties props) {
    Map<String, String> res = new HashMap<>(env);

    final Set<String> keys = props.stringPropertyNames();

    for (String key : keys) {
      if (!key.startsWith(SYS_PROPERTIES_PREFIX)) {
        continue;
      }
      String value = props.getProperty(key);
      if (value == null) {
        continue;
      }
      String envKey = key.toUpperCase().replace(".", "_");
      res.put(envKey, value);

      if (envKey.endsWith("SERVICE_KEY")) {
        value = ServiceKeyUtils.maskServiceKey(value);
      }
      logger.info("System property " + key + "=" + value + ", override " + envKey);
    }
    return res;
  }

  private static void loadConfigurations() throws InvalidConfigException {
    ConfigContainer configs = null;
    boolean hasReadConfigException = false;
    try {
      configs = readConfigs(mergeEnvWithSysProperties(System.getenv(), System.getProperties()));
    } catch (InvalidConfigException e) {
      hasReadConfigException = true;
      // attempt to initialize the logger factory, as it could contain valid logging config and it's
      // valuable to log message to it if possible
      if (e instanceof InvalidConfigReadSourceException) {
        configs =
            ((InvalidConfigReadSourceException) e)
                .getConfigContainerBeforeException(); // try with partial config, even though we
        // will fail the agent (throwing exception),
        // config such as service key is still useful
        // for reporting failure
      }
      throw e; // rethrow the exception
    } finally {
      if (configs != null) {
        maybeFollowOtelConfigProperties(configs);
        ConfigContainer config = configs.subset(ConfigGroup.AGENT);
        LoggerFactory.init(
            LoggingConfigProvider.getLoggerConfiguration(
                config)); // initialize the logger factory as soon as the config is available
        try {
          processConfigs(configs);
          configureOtelLogExport(configs);
          configureOtelMetricExport(configs);
          configureOtelTraceExport(configs);
        } catch (InvalidConfigException e) {
          // if there was a config read exception then processConfigs might throw exception due to
          // incomplete config container.
          // Do NOT override the original exception by not rethrowing the exception
          if (!hasReadConfigException) {
            throw e;
          }
        }
      }
    }
    attachConfigurationFileWatcher();
  }

  /**
   * Collect configuration properties from both the -javaagent arguments and the configuration
   * property file
   *
   * @param env the environment variables
   * @return ConfigContainer filled with the properties parsed from the -javaagent arguments and
   *     configuration property file
   * @throws InvalidConfigException failed to read the configs
   */
  static ConfigContainer readConfigs(Map<String, String> env) throws InvalidConfigException {
    ConfigContainer container = new ConfigContainer();

    List<InvalidConfigException> exceptions = new ArrayList<InvalidConfigException>();

    try {
      // Firstly, read from ENV
      logger.debug("Start reading configs from ENV");
      new EnvConfigReader(env).read(container);
      logger.debug("Finished reading configs from ENV");
    } catch (InvalidConfigException e) {
      exceptions.add(
          new InvalidConfigReadSourceException(
              e.getConfigProperty(), ConfigSourceType.ENV_VAR, null, container, e));
    }

    String location = null;
    // Thirdly, read from Config Property File
    InputStream config = null;
    try {
      if (container.containsProperty(ConfigProperty.AGENT_CONFIG)) {
        location = (String) container.get(ConfigProperty.AGENT_CONFIG);
        try {
          config = new FileInputStream(location);
        } catch (FileNotFoundException e) {
          throw new InvalidConfigException(e);
        }
      } else {
        try { // read from the same directory as the agent jar file
          File jarDirectory =
              new File(
                      ConfigurationLoader.class
                          .getProtectionDomain()
                          .getCodeSource()
                          .getLocation()
                          .toURI())
                  .getParentFile();
          File confFromJarDir = new File(jarDirectory, CONFIG_FILE);
          config = new FileInputStream(confFromJarDir);
          location = confFromJarDir.getPath();
        } catch (URISyntaxException | FileNotFoundException e) {
          logger.debug("Could not find config file in current directory.");
        }
      }
      if (config != null) {
        new JsonConfigReader(config).read(container);
        setWatchedPaths(location, File.separatorChar);
        logger.info("Finished reading configs from config file: " + location);
      }

      new JsonConfigReader(ConfigurationLoader.class.getResourceAsStream("/" + CONFIG_FILE))
          .read(container);
      logger.debug("Finished reading built-in default settings.");

    } catch (InvalidConfigException e) {
      exceptions.add(
          new InvalidConfigReadSourceException(
              e.getConfigProperty(), ConfigSourceType.JSON_FILE, location, container, e));
    } finally {
      if (config != null) {
        try {
          config.close();
        } catch (IOException ignored) {
        }
      }
    }

    if (!exceptions.isEmpty()) {
      // rethrow 1st exceptions encountered
      throw exceptions.get(0);
    }

    // check all the required keys are present
    checkRequiredConfigKeys(container);

    return container;
  }

  // visible for testing
  static void setWatchedPaths(String location, char sep) {
    int lastIndexOf = location.lastIndexOf(sep);
    if (lastIndexOf != -1 && lastIndexOf + 1 < location.length()) {
      configurationFileDir = location.substring(0, lastIndexOf);
      runtimeConfigFilename = location.substring(lastIndexOf + 1);
    }
  }

  // only for testing purposes
  static void resetWatchedPaths() {
    configurationFileDir = null;
    runtimeConfigFilename = null;
  }

  /**
   * Checks whether all the required config keys by this Agent is present.
   *
   * @param configs {@link ConfigContainer}
   * @throws InvalidConfigException if required configs are not specified
   */
  private static void checkRequiredConfigKeys(ConfigContainer configs)
      throws InvalidConfigException {
    Set<ConfigProperty> requiredKeys = new HashSet<ConfigProperty>();

    requiredKeys.add(ConfigProperty.AGENT_SERVICE_KEY);
    requiredKeys.add(ConfigProperty.AGENT_LOGGING);

    requiredKeys.add(ConfigProperty.MONITOR_JMX_ENABLE);
    requiredKeys.add(ConfigProperty.MONITOR_JMX_SCOPES);

    Set<ConfigProperty> missingKeys = new HashSet<ConfigProperty>();

    for (ConfigProperty requiredKey : requiredKeys) {
      if (!configs.containsProperty(requiredKey)) {
        missingKeys.add(requiredKey);
      }
    }

    if (!missingKeys.isEmpty()) {
      throw new InvalidConfigException("Missing Configs " + missingKeys);
    }
  }

  /**
   * Validate and populate the ConfigContainer with defaults if not found from the config loading
   *
   * <p>Then initializes {@link ConfigManager} with the processed values
   *
   * @param configs contains loaded agent configuration
   */
  public static void processConfigs(ConfigContainer configs) throws InvalidConfigException {
    if (configs.containsProperty(ConfigProperty.AGENT_DEBUG)) { // legacy flag
      configs.put(ConfigProperty.AGENT_LOGGING, false);
    }

    if (configs.containsProperty(ConfigProperty.AGENT_SERVICE_KEY)
        && !((String) configs.get(ConfigProperty.AGENT_SERVICE_KEY)).isEmpty()) {
      // Customer access key (UUID)
      String rawServiceKey = (String) configs.get(ConfigProperty.AGENT_SERVICE_KEY);
      String serviceKey = ServiceKeyUtils.transformServiceKey(rawServiceKey);

      if (!serviceKey.equalsIgnoreCase(rawServiceKey)) {
        logger.warn(
            "Invalid service name detected in service key, the service key is transformed to "
                + ServiceKeyUtils.maskServiceKey(serviceKey));
        configs.put(ConfigProperty.AGENT_SERVICE_KEY, serviceKey, true);
      }
      logger.debug("Service key (masked) is [" + ServiceKeyUtils.maskServiceKey(serviceKey) + "]");

    } else {
      if (!configs.containsProperty(ConfigProperty.AGENT_SERVICE_KEY)) {
        logger.warn(
            "Could not find the service key! Please specify "
                + ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey()
                + " in "
                + CONFIG_FILE
                + " or via env variable.");
        throw new InvalidConfigServiceKeyException("Service key not found");
      } else {
        logger.warn(
            "Service key is empty! Please specify "
                + ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey()
                + " in "
                + CONFIG_FILE
                + " or via env variable.");
        throw new InvalidConfigServiceKeyException("Service key is empty");
      }
    }

    TraceConfigs traceConfigs = null;
    if (configs.containsProperty(ConfigProperty.AGENT_TRANSACTION_SETTINGS)) {
      if (configs.containsProperty(ConfigProperty.AGENT_URL_SAMPLE_RATE)) {
        logger.warn(
            ConfigProperty.AGENT_URL_SAMPLE_RATE.getConfigFileKey()
                + " is ignored as "
                + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey()
                + " is also defined");
      }
      traceConfigs = (TraceConfigs) configs.get(ConfigProperty.AGENT_TRANSACTION_SETTINGS);

    } else if (configs.containsProperty(ConfigProperty.AGENT_URL_SAMPLE_RATE)) {
      traceConfigs = (TraceConfigs) configs.get(ConfigProperty.AGENT_URL_SAMPLE_RATE);
    }

    if (traceConfigs != null) {
      configs.put(ConfigProperty.AGENT_INTERNAL_TRANSACTION_SETTINGS, traceConfigs);
    }

    if (configs.containsProperty(ConfigProperty.AGENT_TRANSACTION_NAMING_SCHEMES)) {
      Object schemes = configs.get(ConfigProperty.AGENT_TRANSACTION_NAMING_SCHEMES);
      if (schemes instanceof List<?>) {
        List<TransactionNamingScheme> transactionNamingSchemes =
            ((List<?>) schemes)
                .stream()
                    .filter(scheme -> scheme instanceof TransactionNamingScheme)
                    .map(TransactionNamingScheme.class::cast)
                    .collect(Collectors.toList());
        NamingScheme namingScheme = NamingScheme.createDecisionChain(transactionNamingSchemes);
        TransactionNameManager.setNamingScheme(namingScheme);
      }
    }

    Boolean profilerEnabledFromEnvVar = null;
    if (configs.containsProperty(ConfigProperty.PROFILER_ENABLED_ENV_VAR)) {
      profilerEnabledFromEnvVar = (Boolean) configs.get(ConfigProperty.PROFILER_ENABLED_ENV_VAR);
    }

    Integer profilerIntervalFromEnvVar = null;
    if (configs.containsProperty(ConfigProperty.PROFILER_INTERVAL_ENV_VAR)) {
      profilerIntervalFromEnvVar = (Integer) configs.get(ConfigProperty.PROFILER_INTERVAL_ENV_VAR);
    }

    ProfilerSetting finalProfilerSetting;
    if (configs.containsProperty(ConfigProperty.PROFILER)) {
      ProfilerSetting profilerSettingsFromConfigFile =
          (ProfilerSetting) configs.get(ConfigProperty.PROFILER);
      boolean finalEnabled =
          profilerEnabledFromEnvVar != null
              ? profilerEnabledFromEnvVar
              : profilerSettingsFromConfigFile.isEnabled();

      int finalInterval =
          profilerIntervalFromEnvVar != null
              ? profilerIntervalFromEnvVar
              : profilerSettingsFromConfigFile.getInterval();
      finalProfilerSetting =
          new ProfilerSetting(
              finalEnabled,
              profilerSettingsFromConfigFile.getExcludePackages(),
              finalInterval,
              profilerSettingsFromConfigFile.getCircuitBreakerDurationThreshold(),
              profilerSettingsFromConfigFile.getCircuitBreakerCountThreshold());

    } else if (profilerEnabledFromEnvVar != null || profilerIntervalFromEnvVar != null) {
      finalProfilerSetting =
          new ProfilerSetting(
              profilerEnabledFromEnvVar != null ? profilerEnabledFromEnvVar : false,
              profilerIntervalFromEnvVar != null
                  ? profilerIntervalFromEnvVar
                  : ProfilerSetting.DEFAULT_INTERVAL);

    } else {
      finalProfilerSetting = new ProfilerSetting(false, ProfilerSetting.DEFAULT_INTERVAL);
    }

    // always put a profiler setting as we fall back to a default (disabled) one if no setting
    // exists.
    configs.put(ConfigProperty.PROFILER, finalProfilerSetting, true);

    ConfigManager.initialize(configs);
  }

  private static String setSystemProperty(String key, String value) {
    String propertyValue = getConfigValue(key);
    if (propertyValue == null) {
      System.setProperty(key, value);
      return value;
    }

    return propertyValue;
  }

  private static void setEndpoint(String key, String value, String path) {
    String endpoint = getConfigValue("otel.exporter.otlp.endpoint");
    if (endpoint == null) {
      setSystemProperty(key, value);
    } else {
      setSystemProperty(key, String.format("%s%s", endpoint, path));
    }
  }

  private static String setProtocol(String key) {
    String protocol = getConfigValue("otel.exporter.otlp.protocol");
    if (protocol == null) {
      return setSystemProperty(key, ConfigurationLoader.PROTOCOL);
    }
    return protocol;
  }

  static String normalize(String key) {
    return key.toUpperCase().replace(".", "_");
  }

  private static String getConfigValue(String key) {
    String propertyValue = System.getProperty(key);
    if (propertyValue == null) {
      propertyValue = System.getenv(normalize(key));
    }

    return propertyValue;
  }

  private static boolean isDeclarativeConfigOff() {
    return getConfigValue("otel.experimental.config.file") == null;
  }
}
