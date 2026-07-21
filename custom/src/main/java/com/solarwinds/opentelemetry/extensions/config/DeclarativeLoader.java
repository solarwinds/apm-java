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

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.ConfigContainer;
import com.solarwinds.joboe.config.ConfigGroup;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.config.JavaRuntimeVersionChecker;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.opentelemetry.extensions.LoggingConfigProvider;
import com.solarwinds.opentelemetry.extensions.config.parser.yaml.DeclarativeConfigParser;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import java.util.Arrays;
import java.util.List;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class DeclarativeLoader implements DeclarativeConfigurationCustomizerProvider {
  private static final Logger logger = LoggerFactory.getLogger();

  @Override
  public void customize(DeclarativeConfigurationCustomizer customizer) {
    customizer.addModelCustomizer(
        configurationModel -> {
          loadConfig(configurationModel);
          return configurationModel;
        });
  }

  private void loadConfig(OpenTelemetryConfigurationModel configurationModel) {
    DeclarativeConfigProperties configProperties =
        DeclarativeConfiguration.toConfigProperties(configurationModel);

    // Sources are parsed in order into a single container; the first source to define a given
    // key wins (see ConfigContainer#put), so distribution takes precedence over the
    // instrumentation node.
    List<DeclarativeConfigProperties> sources =
        Arrays.asList(
            getDistributionConfig(configProperties), getInstrumentationConfig(configProperties));

    ConfigContainer configContainer = new ConfigContainer();
    DeclarativeConfigParser declarativeConfigParser = new DeclarativeConfigParser(configContainer);

    for (DeclarativeConfigProperties source : sources) {
      if (source == null || source.getPropertyKeys().isEmpty()) {
        continue;
      }

      try {
        declarativeConfigParser.parse(source);
      } catch (InvalidConfigException e) {
        throw new RuntimeException(e);
      }
    }

    try {
      ConfigurationLoader.processConfigs(configContainer);
      ConfigContainer agentConfig = configContainer.subset(ConfigGroup.AGENT);
      LoggerFactory.init(LoggingConfigProvider.getLoggerConfiguration(agentConfig));

      logger.info("Loaded via declarative config");
    } catch (InvalidConfigException e) {
      throw new RuntimeException(e);
    }

    if (!JavaRuntimeVersionChecker.isJdkVersionSupported()) {
      logger.warn(
          String.format(
              "Profiling is not supported for Java runtime version: %s . The lowest Java version supported for profiling is %s.",
              System.getProperty("java.version"), JavaRuntimeVersionChecker.minVersionSupported));
    }
  }

  private DeclarativeConfigProperties getDistributionConfig(
      DeclarativeConfigProperties configProperties) {
    return configProperties
        .getStructured("distribution", DeclarativeConfigProperties.empty())
        .getStructured("solarwinds");
  }

  private DeclarativeConfigProperties getInstrumentationConfig(
      DeclarativeConfigProperties configProperties) {
    return configProperties
        .getStructured("instrumentation/development", DeclarativeConfigProperties.empty())
        .getStructured("java", DeclarativeConfigProperties.empty())
        .getStructured("solarwinds");
  }
}
