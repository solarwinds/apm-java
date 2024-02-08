package com.solarwinds.opentelemetry.extensions;

import static com.solarwinds.opentelemetry.extensions.initialize.AutoConfigurationCustomizerProviderImpl.isAgentEnabled;
import static com.solarwinds.opentelemetry.extensions.initialize.AutoConfigurationCustomizerProviderImpl.setAgentEnabled;
import static com.solarwinds.joboe.core.util.HostTypeDetector.isLambda;
import static io.opentelemetry.semconv.ResourceAttributes.PROCESS_COMMAND_ARGS;
import static io.opentelemetry.semconv.ResourceAttributes.PROCESS_COMMAND_LINE;
import static io.opentelemetry.semconv.ResourceAttributes.PROCESS_RUNTIME_DESCRIPTION;
import static io.opentelemetry.semconv.ResourceAttributes.PROCESS_RUNTIME_NAME;
import static io.opentelemetry.semconv.ResourceAttributes.PROCESS_RUNTIME_VERSION;
import static io.opentelemetry.semconv.ResourceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ResourceAttributes.TELEMETRY_SDK_LANGUAGE;

import com.solarwinds.opentelemetry.core.AgentState;
import com.solarwinds.opentelemetry.extensions.initialize.AutoConfiguredResourceCustomizer;
import com.google.auto.service.AutoService;
import com.solarwinds.joboe.core.EventImpl;
import com.solarwinds.joboe.core.ReporterFactory;
import com.solarwinds.joboe.core.config.ConfigGroup;
import com.solarwinds.joboe.core.config.ConfigManager;
import com.solarwinds.joboe.core.config.ConfigProperty;
import com.solarwinds.joboe.core.config.InvalidConfigException;
import com.solarwinds.joboe.core.config.ProfilerSetting;
import com.solarwinds.joboe.core.logging.Logger;
import com.solarwinds.joboe.core.logging.LoggerFactory;
import com.solarwinds.joboe.core.profiler.Profiler;
import com.solarwinds.joboe.core.rpc.Client;
import com.solarwinds.joboe.core.rpc.ClientException;
import com.solarwinds.joboe.core.rpc.ClientLoggingCallback;
import com.solarwinds.joboe.core.rpc.ClientManagerProvider;
import com.solarwinds.joboe.core.rpc.RpcClientManager;
import com.solarwinds.joboe.core.settings.SettingsManager;
import com.solarwinds.joboe.core.util.DaemonThreadFactory;
import com.solarwinds.joboe.core.util.HostInfoUtils;
import com.solarwinds.joboe.core.util.HostTypeDetector;
import com.solarwinds.joboe.metrics.MetricsCollector;
import com.solarwinds.joboe.metrics.MetricsMonitor;
import com.solarwinds.joboe.metrics.SystemMonitorController;
import com.solarwinds.joboe.metrics.SystemMonitorFactoryImpl;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Executes startup task after it's safe to do so. See <a
 * href="https://github.com/appoptics/opentelemetry-custom-distro/pull/7">...</a>
 */
@AutoService(AgentListener.class)
public class SolarwindsAgentListener implements AgentListener {
  private static final Logger logger = LoggerFactory.getLogger();

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk openTelemetrySdk) {
    if (isLambda() && isAgentEnabled()) {
      try {
        SettingsManager.initialize();
      } catch (ClientException clientException) {
        logger.warn("Failed to initialized settings", clientException);
      }
      return;
    }

    if (isAgentEnabled() && isUsingSolarwindsSampler(openTelemetrySdk)) {
      executeStartupTasks();
      registerShutdownTasks();
      logger.info(
          "Successfully submitted SolarwindsAPM OpenTelemetry extensions initialization tasks");
    }
  }

  boolean isUsingSolarwindsSampler(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    Sampler sampler =
        autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk().getSdkTracerProvider().getSampler();
    boolean verdict = sampler instanceof SolarwindsSampler;
    setAgentEnabled(verdict);

    if (!verdict) {
      logger.warn(
          "Not using Solarwinds sampler. Configured sampler is: " + sampler.getDescription());
    }
    return verdict;
  }

  private void executeStartupTasks() {
    ExecutorService service =
        Executors.newSingleThreadExecutor(DaemonThreadFactory.newInstance("post-startup-tasks"));

    AgentState.setStartupTasksFuture(
        service.submit(
            () -> {
              try {
                logger.info("Starting startup task");
                // trigger init on the Settings reader
                CountDownLatch settingsLatch = null;

                logger.debug("Initializing HostUtils");
                try {
                  HostInfoUtils.NetworkAddressInfo networkAddressInfo =
                      HostInfoUtils.getNetworkAddressInfo();
                  List<String> ipAddresses =
                      networkAddressInfo != null
                          ? networkAddressInfo.getIpAddresses()
                          : Collections.<String>emptyList();

                  logger.debug(
                      "Detected host id: "
                          + HostInfoUtils.getHostId()
                          + " ip addresses: "
                          + ipAddresses);

                  settingsLatch = SettingsManager.initialize();
                } catch (ClientException e) {
                  logger.warn("Failed to initialize RpcSettingsReader : " + e.getMessage());
                }
                logger.debug("Initialized HostUtils");

                logger.debug("Sending init message");
                reportInit();

                logger.debug("Building reporter");
                EventImpl.setDefaultReporter(
                    ReporterFactory.getInstance()
                        .createHostTypeReporter(
                            RpcClientManager.getClient(RpcClientManager.OperationType.TRACING),
                            HostTypeDetector.getHostType()));
                logger.debug("Built reporter");

                logger.info("Starting System monitor");
                SystemMonitorController.startWithBuilder(
                    () ->
                        new SystemMonitorFactoryImpl(
                            ConfigManager.getConfigs(ConfigGroup.MONITOR)) {
                          @Override
                          protected MetricsMonitor buildMetricsMonitor() {
                            try {
                              MetricsCollector metricsCollector =
                                  new MetricsCollector(
                                      configs,
                                      SolarwindsInboundMetricsSpanProcessor
                                          .buildSpanMetricsCollector());
                              return MetricsMonitor.buildInstance(configs, metricsCollector);
                            } catch (InvalidConfigException | ClientException e) {
                              logger.debug(String.format("Error creating MetricsCollector: %s", e));
                            }
                            return null;
                          }
                        }.buildMonitors());
                logger.debug("Started System monitor");

                ProfilerSetting profilerSetting =
                    (ProfilerSetting) ConfigManager.getConfig(ConfigProperty.PROFILER);
                if (profilerSetting != null && profilerSetting.isEnabled()) {
                  logger.debug("Profiler is enabled, local settings : " + profilerSetting);
                  Profiler.initialize(
                      profilerSetting,
                      ReporterFactory.getInstance()
                          .createRpcReporter(
                              RpcClientManager.getClient(
                                  RpcClientManager.OperationType.PROFILING)));
                } else {
                  logger.debug("Profiler is disabled, local settings : " + profilerSetting);
                }

                // now wait for all the latches (for now there's only one for settings)
                try {
                  if (settingsLatch != null) {
                    settingsLatch.await();
                  }
                } catch (InterruptedException e) {
                  logger.warn(
                      "Failed to wait for settings from RpcSettingsReader : " + e.getMessage());
                }
              } catch (Throwable e) {
                logger.warn("Failed post system startup operations due to : " + e.getMessage(), e);
              }
              logger.info("Startup task completed");
            }));
    service.shutdown();
  }

  /**
   * Reports the agent init message.
   *
   * <p>Only the first call to this method will have effect, all other subsequent invocations will
   * be ignored.
   *
   * <p>If timeout (default as 10 seconds, configurable) is non-zero, block until either the init
   * message is sent or timeout elapsed. Otherwise submit the message to the client and return
   * without blocking.
   */
  private void reportInit() {
    try {
      reportLayerInit();
    } catch (Exception e) {
      logger.warn("Failed to post init message: " + (e.getMessage() != null ? e.getMessage() : e));
    }
  }

  private void reportLayerInit() throws ClientException {
    // Must call buildInitMessage before initializing RPC client, otherwise it might deadlock
    // as discussed in https://github.com/librato/joboe/pull/767
    Map<String, Object> initMessage = buildInitMessage();

    Client rpcClient = RpcClientManager.getClient(RpcClientManager.OperationType.STATUS);
    rpcClient.postStatus(
        Collections.singletonList(initMessage), new ClientLoggingCallback<>("post init message"));
  }

  Map<String, Object> buildInitMessage() {
    Map<String, Object> initMessage = new HashMap<>();
    initMessage.put("__Init", true);

    String version = SolarwindsAgentListener.class.getPackage().getImplementationVersion();
    if (version != null) {
      initMessage.put("APM.Version", version);
    }

    // Capture OTel Resource attributes
    Attributes attributes = AutoConfiguredResourceCustomizer.getResource().getAttributes();
    logger.debug(
        "Resource attributes "
            + attributes.toString().replaceAll("(sw.apm.service.key=)\\S+", "$1****"));

    for (Map.Entry<AttributeKey<?>, Object> keyValue : attributes.asMap().entrySet()) {
      AttributeKey<?> attributeKey = keyValue.getKey();
      String attrName = attributeKey.getKey();
      Object attrValue = keyValue.getValue();

      // Do not set service name in __Init message
      if (attrName.equals(SERVICE_NAME.getKey())) {
        continue;
      }
      // Mask service key if captured in process command line or arg
      if ((attrName.equals(PROCESS_COMMAND_LINE.getKey())
              || attrName.equals(PROCESS_COMMAND_ARGS.getKey()))
          && attrValue.toString().contains("sw.apm.service.key=")) {
        attrValue = attrValue.toString().replaceAll("(sw.apm.service.key=)\\S+", "$1****");
      }
      initMessage.put(attrName, attrValue);
    }

    // Ensure required keys are set
    if (!initMessage.containsKey(TELEMETRY_SDK_LANGUAGE.getKey())) {
      initMessage.put(TELEMETRY_SDK_LANGUAGE.getKey(), "java");
    }
    try {
      if (!initMessage.containsKey(PROCESS_RUNTIME_DESCRIPTION.getKey())) {
        // these three java.vm properties are always available
        initMessage.put(
            PROCESS_RUNTIME_DESCRIPTION.getKey(),
            System.getProperty("java.vm.vendor")
                + " "
                + System.getProperty("java.vm.name")
                + " "
                + System.getProperty("java.vm.version"));
      }
      if (!initMessage.containsKey(PROCESS_RUNTIME_NAME.getKey())) {
        initMessage.put(
            PROCESS_RUNTIME_NAME.getKey(), System.getProperty("java.runtime.name", "unavailable"));
      }
      if (!initMessage.containsKey(PROCESS_RUNTIME_VERSION.getKey())) {
        initMessage.put(
            PROCESS_RUNTIME_VERSION.getKey(),
            System.getProperty("java.runtime.version", "unavailable"));
      }
    } catch (SecurityException exp) {
      logger.warn("Cannot get process runtime information.", exp);
    }

    return initMessage;
  }

  private void registerShutdownTasks() {
    Thread shutdownThread =
        new Thread("SolarwindsAPM-shutdown-hook") {
          @Override
          public void run() {
            SystemMonitorController
                .stop(); // stop system monitors, this might flush extra messages to reporters
            if (EventImpl.getEventReporter() != null) {
              EventImpl.getEventReporter()
                  .close(); // close event reporter properly to give it chance to send out pending
              // events in queue
            }
            ClientManagerProvider
                .closeAllManagers(); // close all rpc client, this should flush out all messages or
            // stop immediately (if not connected)
          }
        };

    shutdownThread.setContextClassLoader(null); // avoid memory leak warning
    Runtime.getRuntime().addShutdownHook(shutdownThread);
  }
}
