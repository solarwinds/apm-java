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

package com.solarwinds.opentelemetry.extensions.initialize;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.config.JavaRuntimeVersionChecker;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.shaded.javax.annotation.Nonnull;
import com.solarwinds.opentelemetry.extensions.MetricExporterCustomizer;
import com.solarwinds.opentelemetry.extensions.ResourceCustomizer;
import com.solarwinds.opentelemetry.extensions.SolarwindsPropertiesSupplier;
import com.solarwinds.opentelemetry.extensions.SolarwindsTracerProviderCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;

/**
 * An implementation of {@link AutoConfigurationCustomizer} which serves as the bootstrap for our
 * distro. It's the first user class loaded by the upstream agent. We use the opportunity to
 * bootstrap our configuration via static code.
 */
@AutoService({AutoConfigurationCustomizerProvider.class})
public class AutoConfigurationCustomizerProviderImpl
    implements AutoConfigurationCustomizerProvider {
  private static final Logger logger = LoggerFactory.getLogger();

  private static boolean agentEnabled;

  static {
    try {
      agentEnabled = JavaRuntimeVersionChecker.isJdkVersionSupported();
      if (agentEnabled) {
        ConfigurationLoader.load();
      } else {
        logger.warn(
            String.format(
                "Unsupported Java runtime version: %s. The lowest Java version supported is %s.",
                System.getProperty("java.version"), JavaRuntimeVersionChecker.minVersionSupported));
      }

    } catch (InvalidConfigException invalidConfigException) {
      logger.warn("Error loading agent config", invalidConfigException);
      agentEnabled = false;
    }

    if (!agentEnabled) {
      logger.warn("Solarwinds' extension is disabled");
    }
  }

  public static boolean isAgentEnabled() {
    return agentEnabled;
  }

  public static void setAgentEnabled(boolean agentEnabled) {
    AutoConfigurationCustomizerProviderImpl.agentEnabled =
        AutoConfigurationCustomizerProviderImpl.agentEnabled && agentEnabled;
  }

  @Override
  public void customize(@Nonnull AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration
        .addPropertiesSupplier(new SolarwindsPropertiesSupplier())
        .addTracerProviderCustomizer(new SolarwindsTracerProviderCustomizer())
        .addMetricExporterCustomizer(new MetricExporterCustomizer())
        .addResourceCustomizer(new ResourceCustomizer());
  }

  @Override
  public int order() {
    // Here, we return Integer.MAX_VALUE to force our extension customization to execute last.
    // See https://github.com/appoptics/solarwinds-apm-java/pull/93#discussion_r1165987329 for more
    // context
    return Integer.MAX_VALUE;
  }
}
