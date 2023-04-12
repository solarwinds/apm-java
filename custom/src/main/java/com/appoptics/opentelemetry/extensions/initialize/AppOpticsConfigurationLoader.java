package com.appoptics.opentelemetry.extensions.initialize;

import com.appoptics.opentelemetry.extensions.initialize.config.BuildConfig;
import com.appoptics.opentelemetry.extensions.initialize.config.ConfigConstants;
import com.appoptics.opentelemetry.extensions.initialize.config.LogFileStringParser;
import com.appoptics.opentelemetry.extensions.initialize.config.LogSettingParser;
import com.appoptics.opentelemetry.extensions.initialize.config.LogTraceIdSettingParser;
import com.appoptics.opentelemetry.extensions.initialize.config.ModeStringToBooleanParser;
import com.appoptics.opentelemetry.extensions.initialize.config.ProfilerSettingParser;
import com.appoptics.opentelemetry.extensions.initialize.config.ProxyConfigParser;
import com.appoptics.opentelemetry.extensions.initialize.config.RangeValidationParser;
import com.appoptics.opentelemetry.extensions.initialize.config.TracingModeParser;
import com.appoptics.opentelemetry.extensions.initialize.config.TransactionSettingsConfigParser;
import com.appoptics.opentelemetry.extensions.initialize.config.UrlSampleRateConfigParser;
import com.tracelytics.joboe.config.ConfigContainer;
import com.tracelytics.joboe.config.ConfigGroup;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.ConfigSourceType;
import com.tracelytics.joboe.config.EnvConfigReader;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.joboe.config.InvalidConfigReadSourceException;
import com.tracelytics.joboe.config.InvalidConfigServiceKeyException;
import com.tracelytics.joboe.config.JsonConfigReader;
import com.tracelytics.joboe.config.ProfilerSetting;
import com.tracelytics.joboe.config.TraceConfigs;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.util.ServiceKeyUtils;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class AppOpticsConfigurationLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger();
    private static final String CONFIG_FILE = "solarwinds-apm-config.json";
    private static final String SYS_PROPERTIES_PREFIX = "sw.apm";
    private static AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk;

    static {
        ConfigProperty.AGENT_LOGGING.setParser(LogSettingParser.INSTANCE);
        ConfigProperty.AGENT_LOG_FILE.setParser(new LogFileStringParser());
        ConfigProperty.AGENT_LOGGING_TRACE_ID.setParser(LogTraceIdSettingParser.INSTANCE);
        ConfigProperty.AGENT_TRACING_MODE.setParser(new TracingModeParser());
        ConfigProperty.AGENT_SQL_QUERY_MAX_LENGTH.setParser(new RangeValidationParser<Integer>(ConfigConstants.MAX_SQL_QUERY_LENGTH_LOWER_LIMIT, ConfigConstants.MAX_SQL_QUERY_LENGTH_UPPER_LIMIT));
        ConfigProperty.AGENT_URL_SAMPLE_RATE.setParser(UrlSampleRateConfigParser.INSTANCE);
        ConfigProperty.AGENT_TRANSACTION_SETTINGS.setParser(TransactionSettingsConfigParser.INSTANCE);
        ConfigProperty.AGENT_TRIGGER_TRACE_ENABLED.setParser(ModeStringToBooleanParser.INSTANCE);
        ConfigProperty.AGENT_PROXY.setParser(ProxyConfigParser.INSTANCE);
        ConfigProperty.PROFILER.setParser(ProfilerSettingParser.INSTANCE);
    }

    public static void load() throws InvalidConfigException {
        LOGGER.info(String.format("Otel agent version: %s", BuildConfig.OTEL_AGENT_VERSION));
        LOGGER.info(String.format("Solarwinds agent version: %s", BuildConfig.SOLARWINDS_AGENT_VERSION));
        loadConfigurations();

        String serviceKey = (String) ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY);
        LOGGER.info("Successfully initialized SolarwindsAPM OpenTelemetry extensions with service key " + ServiceKeyUtils.maskServiceKey(serviceKey));
    }

    /**
     * Checks the OpenTelemetry Java agent's logger settings. If the NH custom distro doesn't set a log file
     * but the Otel has this config option, we just follow the Otel's config.
     * @param configs
     */
    private static void maybeFollowOtelConfigProperties(ConfigContainer configs) {
        if (configs.get(ConfigProperty.AGENT_LOG_FILE) == null
        && System.getProperty("io.opentelemetry.javaagent.slf4j.simpleLogger.logFile") != null) {
            try {
                Path path = Paths.get(System.getProperty("io.opentelemetry.javaagent.slf4j.simpleLogger.logFile"));
                configs.put(ConfigProperty.AGENT_LOG_FILE, path);
            } catch (InvalidConfigException | InvalidPathException e) {
                LOGGER.info("failed to follow Otel's log file config." + e.getMessage());
            }
        }
    }

    private static Map<String, String> mergeEnvWithSysProperties(Map<String, String> env, Properties props) {
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
            LOGGER.info("System property " + key + "=" + value + ", override " + envKey);
        }
        return res;
    }

    private static void loadConfigurations() throws InvalidConfigException {
        ConfigContainer configs = null;
        boolean hasReadConfigException = false;
        try {
            configs = readConfigs(mergeEnvWithSysProperties(System.getenv(), System.getProperties()));
        }
        catch (InvalidConfigException e) {
            hasReadConfigException = true;
            //attempt to initialize the logger factory, as it could contain valid logging config and it's valuable to log message to it if possible
            if (e instanceof InvalidConfigReadSourceException) {
                configs = ((InvalidConfigReadSourceException) e).getConfigContainerBeforeException(); //try with partial config, even though we will fail the agent (throwing exception), config such as service key is still useful for reporting failure
            }
            throw e; //rethrow the exception
        } finally {
            if (configs != null) {
                maybeFollowOtelConfigProperties(configs);
                LoggerFactory.init(configs.subset(ConfigGroup.AGENT)); //initialize the logger factory as soon as the config is available
                try {
                    processConfigs(configs);
                }
                catch (InvalidConfigException e) {
                    //if there was a config read exception then processConfigs might throw exception due to incomplete config container.
                    //Do NOT override the original exception by not rethrowing the exception
                    if (!hasReadConfigException) {
                        throw e;
                    }
                }
            }
        }
    }


    /**
     * Collect configuration properties from both the -javaagent arguments and the configuration property file
     *
     *
     * @param env						the environment variables
     * @return                          ConfigContainer filled with the properties parsed from the -javaagent arguments and configuration property file
     * @throws InvalidConfigException   failed to read the configs
     */
    static ConfigContainer readConfigs(Map<String, String> env) throws InvalidConfigException {
        ConfigContainer container = new ConfigContainer();

        List<InvalidConfigException> exceptions = new ArrayList<InvalidConfigException>();

        try {
            //Firstly, read from ENV
            LOGGER.debug("Start reading configs from ENV");
            new EnvConfigReader(env).read(container);
            LOGGER.debug("Finished reading configs from ENV");
        }
        catch (InvalidConfigException e) {
            exceptions.add(new InvalidConfigReadSourceException(e.getConfigProperty(), ConfigSourceType.ENV_VAR, null, container, e));
        }

        String location = null;
        //Thirdly, read from Config Property File
        InputStream config = null;
        try {
            if (container.containsProperty(ConfigProperty.AGENT_CONFIG)) {
                location = (String) container.get(ConfigProperty.AGENT_CONFIG);
                try {
                    config = new FileInputStream(location);
                }
                catch (FileNotFoundException e) {
                    throw new InvalidConfigException(e);
                }
            } else {
                try { // read from the same directory as the agent jar file
                    File jarDirectory = new File(AppOpticsConfigurationLoader.class.getProtectionDomain().getCodeSource().getLocation()
                            .toURI()).getParentFile();
                    File confFromJarDir = new File(jarDirectory, CONFIG_FILE);
                    config = new FileInputStream(confFromJarDir);
                    location = confFromJarDir.getPath();
                } catch (URISyntaxException | FileNotFoundException e) {
                    LOGGER.debug("Could not find config file in current directory.");
                }
            }
            if (config != null) {
                new JsonConfigReader(config).read(container);
                LOGGER.info("Finished reading configs from config file: " + location);
            }

            new JsonConfigReader(AppOpticsConfigurationLoader.class.getResourceAsStream("/" + CONFIG_FILE)).read(container);
            LOGGER.debug("Finished reading built-in default settings.");
        }
        catch (InvalidConfigException e) {
            exceptions.add(new InvalidConfigReadSourceException(e.getConfigProperty(), ConfigSourceType.JSON_FILE, location, container, e));
        } finally {
            if (config != null) {
                try {
                    config.close();
                } catch (IOException ignored) {
                }
            }
        }

        if (!exceptions.isEmpty()) {
            //rethrow 1st exceptions encountered
            throw exceptions.get(0);
        }

        //check all the required keys are present
        checkRequiredConfigKeys(container);

        return container;
    }

    /**
     * Checks whether all the required config keys by this Agent is present.
     *
     * @param configs
     * @throws InvalidConfigException
     */
    private static void checkRequiredConfigKeys(ConfigContainer configs) throws InvalidConfigException {
        Set<ConfigProperty> requiredKeys = new HashSet<ConfigProperty>();

        requiredKeys.add(ConfigProperty.AGENT_SERVICE_KEY);
        requiredKeys.add(ConfigProperty.AGENT_LOGGING);
        requiredKeys.add(ConfigProperty.AGENT_JDBC_INST_ALL);

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
     * Then initializes {@link ConfigManager} with the processed values
     *
     * @param configs
     */
    private static void processConfigs(ConfigContainer configs) throws InvalidConfigException {
        if (configs.containsProperty(ConfigProperty.AGENT_DEBUG)) { //legacy flag
            configs.put(ConfigProperty.AGENT_LOGGING, false);
        }

        if (configs.containsProperty(ConfigProperty.AGENT_SAMPLE_RATE)) {
            Integer sampleRateFromConfig = (Integer) configs.get(ConfigProperty.AGENT_SAMPLE_RATE);
            if (sampleRateFromConfig < 0 ||  sampleRateFromConfig > ConfigConstants.SAMPLE_RESOLUTION) {
                LOGGER.warn(ConfigProperty.AGENT_SAMPLE_RATE + ": Invalid argument value: " + sampleRateFromConfig + ": must be between 0 and " + ConfigConstants.SAMPLE_RESOLUTION);
                throw new InvalidConfigException("Invalid " + ConfigProperty.AGENT_SAMPLE_RATE.getConfigFileKey() + " : " + sampleRateFromConfig);
            }
        }

        if (configs.containsProperty(ConfigProperty.AGENT_SERVICE_KEY) && !"".equals(configs.get(ConfigProperty.AGENT_SERVICE_KEY))) {
            // Customer access key (UUID)
            String rawServiceKey = (String) configs.get(ConfigProperty.AGENT_SERVICE_KEY);
            String serviceKey = ServiceKeyUtils.transformServiceKey(rawServiceKey);

            if (!serviceKey.equalsIgnoreCase(rawServiceKey)) {
                LOGGER.warn("Invalid service name detected in service key, the service key is transformed to " + ServiceKeyUtils.maskServiceKey(serviceKey));
                configs.put(ConfigProperty.AGENT_SERVICE_KEY, serviceKey, true);
            }
            LOGGER.debug("Service key (masked) is [" + ServiceKeyUtils.maskServiceKey(serviceKey) + "]");

        }
        else {
            if (!configs.containsProperty(ConfigProperty.AGENT_SERVICE_KEY)) {
                LOGGER.warn("Could not find the service key! Please specify " + ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey() + " in " + CONFIG_FILE + " or via env variable.");
                throw new InvalidConfigServiceKeyException("Service key not found");
            }
            else {
                LOGGER.warn("Service key is empty! Please specify " + ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey() + " in " + CONFIG_FILE + " or via env variable.");
                throw new InvalidConfigServiceKeyException("Service key is empty");
            }
        }


        if (!configs.containsProperty(ConfigProperty.AGENT_JDBC_INST_ALL)) {
            configs.put(ConfigProperty.AGENT_JDBC_INST_ALL, false);
        }

        TraceConfigs traceConfigs = null;
        if (configs.containsProperty(ConfigProperty.AGENT_TRANSACTION_SETTINGS)) {
            if (configs.containsProperty(ConfigProperty.AGENT_URL_SAMPLE_RATE)) {
                LOGGER.warn(ConfigProperty.AGENT_URL_SAMPLE_RATE.getConfigFileKey() + " is ignored as " + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey() + " is also defined");
            }
            traceConfigs = (TraceConfigs) configs.get(ConfigProperty.AGENT_TRANSACTION_SETTINGS);
        }
        else if (configs.containsProperty(ConfigProperty.AGENT_URL_SAMPLE_RATE)) {
            traceConfigs = (TraceConfigs) configs.get(ConfigProperty.AGENT_URL_SAMPLE_RATE);
        }

        if (traceConfigs != null) {
            configs.put(ConfigProperty.AGENT_INTERNAL_TRANSACTION_SETTINGS, traceConfigs);
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
            ProfilerSetting profilerSettingsFromConfigFile = (ProfilerSetting) configs.get(ConfigProperty.PROFILER);
            boolean finalEnabled = profilerEnabledFromEnvVar != null ? profilerEnabledFromEnvVar : profilerSettingsFromConfigFile.isEnabled();
            int finalInterval = profilerIntervalFromEnvVar != null ? profilerIntervalFromEnvVar : profilerSettingsFromConfigFile.getInterval();
            finalProfilerSetting = new ProfilerSetting(finalEnabled, profilerSettingsFromConfigFile.getExcludePackages(), finalInterval, profilerSettingsFromConfigFile.getCircuitBreakerDurationThreshold(), profilerSettingsFromConfigFile.getCircuitBreakerCountThreshold());
        }
        else if (profilerEnabledFromEnvVar != null || profilerIntervalFromEnvVar != null) {
            finalProfilerSetting = new ProfilerSetting(profilerEnabledFromEnvVar != null ? profilerEnabledFromEnvVar : false, profilerIntervalFromEnvVar != null ? profilerIntervalFromEnvVar : ProfilerSetting.DEFAULT_INTERVAL);
        }
        else {
            finalProfilerSetting = new ProfilerSetting(false, ProfilerSetting.DEFAULT_INTERVAL);
        }

        // always put a profiler setting as we fall back to a default (disabled) one if no setting exists.
        configs.put(ConfigProperty.PROFILER, finalProfilerSetting, true);

        ConfigManager.initialize(configs);
    }
}