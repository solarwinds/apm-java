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

package com.solarwinds.opentelemetry.extensions;

import static com.solarwinds.opentelemetry.extensions.config.provider.AutoConfigurationCustomizerProviderImpl.isAgentEnabled;
import static com.solarwinds.opentelemetry.extensions.config.provider.AutoConfigurationCustomizerProviderImpl.setAgentEnabled;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.ConfigGroup;
import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.core.ReporterFactory;
import com.solarwinds.joboe.core.profiler.Profiler;
import com.solarwinds.joboe.core.profiler.ProfilerSetting;
import com.solarwinds.joboe.core.rpc.ClientException;
import com.solarwinds.joboe.core.rpc.ClientManagerProvider;
import com.solarwinds.joboe.core.rpc.RpcClientManager;
import com.solarwinds.joboe.core.util.DaemonThreadFactory;
import com.solarwinds.joboe.core.util.HostInfoUtils;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.metrics.MetricsCollector;
import com.solarwinds.joboe.metrics.MetricsMonitor;
import com.solarwinds.joboe.metrics.SystemMonitorController;
import com.solarwinds.joboe.metrics.SystemMonitorFactoryImpl;
import com.solarwinds.joboe.sampling.SettingsManager;
import com.solarwinds.opentelemetry.core.AgentState;
import com.solarwinds.opentelemetry.extensions.config.HttpSettingsFetcher;
import com.solarwinds.opentelemetry.extensions.config.HttpSettingsReader;
import com.solarwinds.opentelemetry.extensions.config.HttpSettingsReaderDelegate;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Collections;
import java.util.List;
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
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    if (isAgentEnabled() && isUsingSolarwindsSampler(autoConfiguredOpenTelemetrySdk)) {
      executeStartupTasks();
      registerShutdownTasks();
      logger.info(
          "Successfully submitted SolarwindsAPM OpenTelemetry extensions initialization tasks");
    } else {
      autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk().shutdown();
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

                logger.debug("Initializing HostUtils");
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

                CountDownLatch settingsLatch =
                    SettingsManager.initialize(
                        new HttpSettingsFetcher(
                            new HttpSettingsReader(new HttpSettingsReaderDelegate()), 60),
                        SamplingConfigProvider.getSamplingConfiguration());
                logger.debug("Initialized HostUtils");

                logger.info("Starting System monitor");
                SystemMonitorController.startWithBuilder(
                    () ->
                        new SystemMonitorFactoryImpl(
                            ConfigManager.getConfigs(ConfigGroup.MONITOR)) {
                          @Override
                          protected MetricsMonitor buildMetricsMonitor() {
                            try {
                              MetricsCollector metricsCollector =
                                  new MetricsCollector(configs, null);

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
                          .createQueuingEventReporter(
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

  private void registerShutdownTasks() {
    Thread shutdownThread =
        new Thread("SolarwindsAPM-shutdown-hook") {
          @Override
          public void run() {
            SystemMonitorController
                .stop(); // stop system monitors, this might flush extra messages to reporters
            if (ReporterProvider.getEventReporter() != null) {
              ReporterProvider.getEventReporter()
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
