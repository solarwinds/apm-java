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

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.config.ServiceKeyUtils;
import com.solarwinds.joboe.logging.LoggerFactory;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.ServiceAttributes;
import io.opentelemetry.semconv.incubating.CloudIncubatingAttributes;
import io.opentelemetry.semconv.incubating.ProcessIncubatingAttributes;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.Getter;

public class ResourceCustomizer implements BiFunction<Resource, ConfigProperties, Resource> {

  @Getter private static Resource resource;

  @Override
  public Resource apply(Resource resource, ConfigProperties configProperties) {
    ResourceBuilder resourceBuilder = resource.toBuilder();
    String resourceAttribute =
        resource.getAttribute(ProcessIncubatingAttributes.PROCESS_COMMAND_LINE);
    List<String> processArgs =
        resource.getAttribute(ProcessIncubatingAttributes.PROCESS_COMMAND_ARGS);

    if (resourceAttribute != null) {
      resourceBuilder.put(
          ProcessIncubatingAttributes.PROCESS_COMMAND_LINE,
          resourceAttribute.replaceAll("(sw.apm.service.key=)\\S+", "$1****"));
    }

    if (processArgs != null) {
      List<String> args =
          processArgs.stream()
              .map(arg -> arg.replaceAll("(sw.apm.service.key=)\\S+", "$1****"))
              .collect(Collectors.toList());
      resourceBuilder.put(ProcessIncubatingAttributes.PROCESS_COMMAND_ARGS, args);
    }

    String serviceName = getServiceName(resource, configProperties);
    updateServiceKey(serviceName);
    resourceBuilder.put(ServiceAttributes.SERVICE_NAME, serviceName);

    LoggerFactory.getLogger().info("Resolved service name: " + serviceName);
    ResourceCustomizer.resource = resourceBuilder.build();
    return ResourceCustomizer.resource;
  }

  private String getServiceName(Resource resource, ConfigProperties configProperties) {
    String serviceKeyName = null;
    String serviceKey = ConfigManager.getConfigOptional(ConfigProperty.AGENT_SERVICE_KEY, null);
    if (serviceKey != null) {
      serviceKeyName = ServiceKeyUtils.getServiceName(serviceKey);
    }

    // Only allow detected name for azure app service
    if (!"azure.app_service".equals(resource.getAttribute(CloudIncubatingAttributes.CLOUD_PLATFORM))
        && configProperties.getString("otel.service.name") == null) {
      return serviceKeyName;
    }

    String serviceName = resource.getAttribute(ServiceAttributes.SERVICE_NAME);
    if (serviceKeyName != null && (serviceName == null || serviceName.startsWith("unknown_"))) {
      serviceName = serviceKeyName;
    }

    return serviceName;
  }

  private void updateServiceKey(String serviceName) {
    if (serviceName != null) {
      String serviceKey = ConfigManager.getConfigOptional(ConfigProperty.AGENT_SERVICE_KEY, ":");
      String apiKey = ServiceKeyUtils.getApiKey(serviceKey);

      try {
        ConfigManager.setConfig(
            ConfigProperty.AGENT_SERVICE_KEY, String.format("%s:%s", apiKey, serviceName));
      } catch (InvalidConfigException ignore) {
        LoggerFactory.getLogger().debug("Failed to update service key with name: " + serviceName);
      }
    }
  }
}
