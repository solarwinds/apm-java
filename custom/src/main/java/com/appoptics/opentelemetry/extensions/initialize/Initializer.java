package com.appoptics.opentelemetry.extensions.initialize;

import com.appoptics.opentelemetry.extensions.AppOpticsInboundMetricsSpanProcessor;
import com.appoptics.opentelemetry.extensions.initialize.config.*;
import com.tracelytics.joboe.EventImpl;
import com.tracelytics.joboe.RpcEventReporter;
import com.tracelytics.joboe.config.*;
import com.tracelytics.joboe.rpc.*;
import com.tracelytics.joboe.settings.SettingsManager;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.monitor.SystemMonitor;
import com.tracelytics.monitor.SystemMonitorBuilder;
import com.tracelytics.monitor.SystemMonitorController;
import com.tracelytics.monitor.SystemMonitorFactoryImpl;
import com.tracelytics.monitor.metrics.MetricsCollector;
import com.tracelytics.monitor.metrics.MetricsMonitor;
import com.tracelytics.profiler.Profiler;
import com.tracelytics.util.*;
import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Initializer {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Initializer.class.getName());
    private static final String VERSION_PROPERTIES_FILE = "/version.properties";
    static {
        ConfigProperty.AGENT_LOGGING.setParser(LogSettingParser.INSTANCE);
        ConfigProperty.AGENT_LOG_FILE.setParser(new LogFileStringParser());
        ConfigProperty.AGENT_LOGGING_TRACE_ID.setParser(LogTraceIdSettingParser.INSTANCE);
        ConfigProperty.AGENT_TRACING_MODE.setParser(new TracingModeParser());
        ConfigProperty.AGENT_SQL_QUERY_MAX_LENGTH.setParser(new RangeValidationParser<Integer>(ConfigConstants.MAX_SQL_QUERY_LENGTH_LOWER_LIMIT, ConfigConstants.MAX_SQL_QUERY_LENGTH_UPPER_LIMIT));
        ConfigProperty.AGENT_URL_SAMPLE_RATE.setParser(UrlSampleRateConfigParser.INSTANCE);
//        ConfigProperty.AGENT_BACKTRACE_MODULES.setParser(ModulesParser.INSTANCE);
//        ConfigProperty.AGENT_EXTENDED_BACK_TRACES_BY_MODULE.setParser(ModulesParser.INSTANCE);
//        ConfigProperty.AGENT_HIDE_PARAMS.setParser(new HideParamsConfigParser());
        ConfigProperty.AGENT_TRANSACTION_SETTINGS.setParser(TransactionSettingsConfigParser.INSTANCE);
        ConfigProperty.AGENT_TRIGGER_TRACE_ENABLED.setParser(ModeStringToBooleanParser.INSTANCE);
        ConfigProperty.AGENT_PROXY.setParser(ProxyConfigParser.INSTANCE);
        ConfigProperty.PROFILER.setParser(ProfilerSettingParser.INSTANCE);
    }

    public static Future<?> initialize(String serviceKey) throws InvalidConfigException {
        InvalidConfigException exception = null;
        Future<?> future = null;
        try {
            initializeConfig(serviceKey);
            future = executeStartupTasks();
            registerShutdownTasks();
        } catch (InvalidConfigException e) {
            exception = e;
            throw e;
        } finally {
            reportInit(exception);
            return future;
        }
    }

    private static void registerShutdownTasks() {
        Thread shutdownThread = new Thread("AppOptics-shutdown-hook") {
            @Override
            public void run() {
                SystemMonitorController.stop(); //stop system monitors, this might flush extra messages to reporters
                if (EventImpl.getEventReporter() != null) {
                    EventImpl.getEventReporter().close(); //close event reporter properly to give it chance to send out pending events in queue
                }
                RpcClientManager.closeAllManagers(); //close all rpc client, this should flush out all messages or stop immediately (if not connected)
            }
        };

        shutdownThread.setContextClassLoader(null); //avoid memory leak warning

        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }

    public static Future<?> executeStartupTasks() {
        ExecutorService service = Executors.newSingleThreadExecutor(DaemonThreadFactory.newInstance("post-startup-tasks"));

        Future<?> future = service.submit(new Runnable() {
            public void run() {
                try {
                    // trigger init on the Settings reader
                    CountDownLatch settingsLatch = null;

                    HostInfoUtils.init(ServerHostInfoReader.INSTANCE);
                    try {
                        HostInfoUtils.NetworkAddressInfo networkAddressInfo = HostInfoUtils.getNetworkAddressInfo();
                        List<String> ipAddresses = networkAddressInfo != null ? networkAddressInfo.getIpAddresses() : Collections.<String>emptyList();

                        logger.debug("Detected host id: " + HostInfoUtils.getHostId() + " ip addresses: " + ipAddresses);

                        settingsLatch = SettingsManager.initialize();
                    } catch (ClientException e) {
                        logger.debug("Failed to initialize RpcSettingsReader : " + e.getMessage());
                    }

                    EventImpl.setDefaultReporter(RpcEventReporter.buildReporter(RpcClientManager.OperationType.TRACING));

                    SystemMonitorController.startWithBuilder(new SystemMonitorBuilder() {
                        @Override
                        public List<SystemMonitor<?, ?>> build() {
                            return new SystemMonitorFactoryImpl(ConfigManager.getConfigs(ConfigGroup.MONITOR)) {
                                @Override
                                protected MetricsMonitor buildMetricsMonitor() {
                                    try {
                                        MetricsCollector metricsCollector = new MetricsCollector(configs, AppOpticsInboundMetricsSpanProcessor.buildSpanMetricsCollector());
                                        return MetricsMonitor.buildInstance(configs, metricsCollector);
                                    } catch (InvalidConfigException e) {
                                        e.printStackTrace();
                                    } catch (ClientException e) {
                                        e.printStackTrace();
                                    }
                                    return null;
                                }
                            }.buildMonitors();
                        }
                    });
                    //SystemMonitorController.start();

                    ProfilerSetting profilerSetting = (ProfilerSetting) ConfigManager.getConfig(ConfigProperty.PROFILER);
                    if (profilerSetting != null && profilerSetting.isEnabled()) {
                        logger.debug("Profiler is enabled, local settings : " + profilerSetting);
                        Profiler.initialize(profilerSetting, RpcEventReporter.buildReporter(RpcClientManager.OperationType.PROFILING));
                    } else {
                        logger.debug("Profiler is disabled, local settings : " + profilerSetting);
                    }

                    //now wait for all the latches (for now there's only one for settings)
                    try {
                        if (settingsLatch != null) {
                            settingsLatch.await();
                        }
                    } catch (InterruptedException e) {
                        logger.debug("Failed to wait for settings from RpcSettingsReader : " + e.getMessage());
                    }
                } catch (Throwable e) {
                    logger.warn("Failed post system startup operations due to : " + e.getMessage(), e);
                }
            }
        });

        service.shutdown();

        return future;
    }

    private static void initializeConfig(String serviceKey) throws InvalidConfigException {
        ConfigContainer configs = null;
        boolean hasReadConfigException = false;
        try {
            configs = readConfigs(System.getenv(), serviceKey);
        } catch (InvalidConfigException e) {
            hasReadConfigException = true;
            //attempt to initialize the logger factory, as it could contain valid logging config and it's valuable to log message to it if possible
            if (e instanceof InvalidConfigReadSourceException) {
                configs = ((InvalidConfigReadSourceException) e).getConfigContainerBeforeException(); //try with partial config, even though we will fail the agent (throwing exception), config such as service key is still useful for reporting failure
            }
            throw e; //rethrow the exception
        } finally {
            if (configs != null) {
                LoggerFactory.init(configs.subset(ConfigGroup.AGENT)); //initialize the logger factory as soon as the config is available
                try {
                    processConfigs(configs);
                } catch (InvalidConfigException e) {
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
     * @param explicitServiceKey        an explicit service key provided by the caller, this will have higher precedence
     * @return                          ConfigContainer filled with the properties parsed from the -javaagent arguments and configuration property file
     * @throws InvalidConfigException   failed to read the configs
     */
    static ConfigContainer readConfigs(Map<String, String> env, String explicitServiceKey) throws InvalidConfigException {
        ConfigContainer container = new ConfigContainer();

        if (explicitServiceKey != null) {
            container.putByStringValue(ConfigProperty.AGENT_SERVICE_KEY, explicitServiceKey);
        }

        List<InvalidConfigException> exceptions = new ArrayList<InvalidConfigException>();

        try {
            //Firstly, read from ENV
            logger.debug("Start reading configs from ENV");
            new EnvConfigReader(env).read(container);
            logger.debug("Finished reading configs from ENV");
        } catch (InvalidConfigException e) {
            exceptions.add(new InvalidConfigReadSourceException(e.getConfigProperty(), ConfigSourceType.ENV_VAR, null, container, e));
        }

//        try {
//            //Secondly, read from Java Agent Arguments
//            logger.fine("Start reading configs from -javaagent arguments");
//            new JavaAgentArgumentConfigReader(agentArgs).read(container);
//            logger.fine("Finished reading configs from -javaagent arguments");
//        } catch (InvalidConfigException e) {
//            exceptions.add(new InvalidConfigReadSourceException(e.getConfigProperty(), ConfigSourceType.JVM_ARG, null, container, e));
//        }


        String location = null;
        try {
            //Thirdly, read from Config Property File
            InputStream config;
            if (container.containsProperty(ConfigProperty.AGENT_CONFIG)) {
                location = (String) container.get(ConfigProperty.AGENT_CONFIG);
                try {
                    config = new FileInputStream(location);
                } catch (FileNotFoundException e) {
                    throw new InvalidConfigException(e);
                }
            } else {
                config = Initializer.class.getResourceAsStream("/javaagent.json"); //the file included within the jar
                location = "default";

            }
            new JsonConfigReader(config).read(container);
            logger.debug("Finished reading configs from config file: " + location);
        } catch (InvalidConfigException e) {
            exceptions.add(new InvalidConfigReadSourceException(e.getConfigProperty(), ConfigSourceType.JSON_FILE, location, container, e));
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
            StringBuffer errorMessage = new StringBuffer("Missing Configs ");
            errorMessage.append(missingKeys.toString());
            throw new InvalidConfigException(errorMessage.toString());
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
                logger.warn(ConfigProperty.AGENT_SAMPLE_RATE + ": Invalid argument value: " + sampleRateFromConfig + ": must be between 0 and " + ConfigConstants.SAMPLE_RESOLUTION);
                throw new InvalidConfigException("Invalid " + ConfigProperty.AGENT_SAMPLE_RATE.getConfigFileKey() + " : " + sampleRateFromConfig);
            }
        }

        if (configs.containsProperty(ConfigProperty.AGENT_SERVICE_KEY) && !"".equals(configs.get(ConfigProperty.AGENT_SERVICE_KEY))) {
            // Customer access key (UUID)
            String rawServiceKey = (String) configs.get(ConfigProperty.AGENT_SERVICE_KEY);
            String serviceKey = ServiceKeyUtils.transformServiceKey(rawServiceKey);

            if (!serviceKey.equalsIgnoreCase(rawServiceKey)) {
                logger.warn("Invalid service name detected in service key, the service key is transformed to " + ServiceKeyUtils.maskServiceKey(serviceKey));
                configs.put(ConfigProperty.AGENT_SERVICE_KEY, serviceKey, true);
            }
            logger.debug("Service key (masked) is [" + ServiceKeyUtils.maskServiceKey(serviceKey) + "]");

        } else {
            if (!configs.containsProperty(ConfigProperty.AGENT_SERVICE_KEY)) {
                logger.warn("Could not find the service key! Please specify " + ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey() + " in javaagent.json");
                throw new InvalidConfigServiceKeyException("Service key not found");
            } else {
                logger.warn("Service key is empty! Please specify " + ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey() + " in javaagent.json");
                throw new InvalidConfigServiceKeyException("Service key is empty");
            }
        }


        if (!configs.containsProperty(ConfigProperty.AGENT_JDBC_INST_ALL)) {
            configs.put(ConfigProperty.AGENT_JDBC_INST_ALL, false);
        }

        TraceConfigs traceConfigs = null;
        if (configs.containsProperty(ConfigProperty.AGENT_TRANSACTION_SETTINGS)) {
            if (configs.containsProperty(ConfigProperty.AGENT_URL_SAMPLE_RATE)) {
                logger.warn(ConfigProperty.AGENT_URL_SAMPLE_RATE.getConfigFileKey() + " is ignored as " + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey() + " is also defined");
            }
            traceConfigs = (TraceConfigs) configs.get(ConfigProperty.AGENT_TRANSACTION_SETTINGS);
        } else if (configs.containsProperty(ConfigProperty.AGENT_URL_SAMPLE_RATE)) {
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

        ProfilerSetting finalProfilerSetting = null;
        if (configs.containsProperty(ConfigProperty.PROFILER)) {
            ProfilerSetting profilerSettingsFromConfigFile = (ProfilerSetting) configs.get(ConfigProperty.PROFILER);
            boolean finalEnabled = profilerEnabledFromEnvVar != null ? profilerEnabledFromEnvVar : profilerSettingsFromConfigFile.isEnabled();
            int finalInterval = profilerIntervalFromEnvVar != null ? profilerIntervalFromEnvVar : profilerSettingsFromConfigFile.getInterval();
            finalProfilerSetting = new ProfilerSetting(finalEnabled, profilerSettingsFromConfigFile.getExcludePackages(), finalInterval, profilerSettingsFromConfigFile.getCircuitBreakerDurationThreshold(), profilerSettingsFromConfigFile.getCircuitBreakerCountThreshold());
        } else if (profilerEnabledFromEnvVar != null || profilerIntervalFromEnvVar != null) {
            finalProfilerSetting = new ProfilerSetting(profilerEnabledFromEnvVar != null ? profilerEnabledFromEnvVar : false, profilerIntervalFromEnvVar != null ? profilerIntervalFromEnvVar : ProfilerSetting.DEFAULT_INTERVAL);
        }

        if (finalProfilerSetting != null) {
            configs.put(ConfigProperty.PROFILER, finalProfilerSetting, true);
        }

        ConfigManager.initialize(configs);
    }

    /**
     * Reports the agent init message.
     *
     * Only the first call to this method will have effect, all other subsequent invocations will be ignored.
     *
     * If timeout (default as 10 seconds, configurable) is non-zero, block until either the init message is sent or timeout elapsed. Otherwise submit the message to the client and return without blocking.
     *
     */
    private static Future<Result> reportInit(Throwable configException) {
        Future<Result> future = null;
        try {
            String layerName = (String) ConfigManager.getConfig(ConfigProperty.AGENT_LAYER);
            Properties versionsProperties = new Properties();
            versionsProperties.load(Initializer.class.getResourceAsStream(VERSION_PROPERTIES_FILE));
            String version = versionsProperties.getProperty("agent.version");
            if (version == null) {
                logger.warn("Could not locate agent.version in " + VERSION_PROPERTIES_FILE + " for version...");
            }

            future = reportLayerInit(layerName, version, configException);
        } catch (Exception e) {
            logger.warn("Failed to post init message: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
            if (configException != null) {
                logger.warn("Failed to report init error [" + configException.getMessage() + "]");
            }
        }
        return future;
    }

    private static Future<Result> reportLayerInit(final String layer, final String version, final Throwable configException) throws ClientException {
        //must call buildInitMessage before initializing RPC client, otherwise it might deadlock as discussed in https://github.com/librato/joboe/pull/767
        Map<String, Object> initMessage = buildInitMessage(layer, version, configException);

        Client rpcClient = RpcClientManager.getClient(RpcClientManager.OperationType.STATUS);
        return rpcClient.postStatus(Collections.singletonList(initMessage), new ClientLoggingCallback<Result>("post init message"));
    }

    static Map<String, Object> buildInitMessage(String layer, String version, Throwable configException) {
        Map<String, Object> initMessage = new HashMap<String, Object>();

        initMessage.put("Layer", layer);
        initMessage.put("Label", "single");
        initMessage.put("__Init", true);

        if (version != null) {
            initMessage.put("Java.AppOptics.Opentelemetry.Version", version);
        }

        String javaVersion = System.getProperty("java.version");
        if (javaVersion != null) {
            initMessage.put("Java.Version", javaVersion);
        }

        if (configException != null) {
            initMessage.put("Error", configException.getClass().getName() + ":" + configException.getMessage());
        }

        initMessage.put("Java.LastRestart", TimeUtils.getTimestampMicroSeconds());

        return initMessage;
    }

}
