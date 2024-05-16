/*
 * Copyright SolarWinds Worldwide, LLC.
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

import com.solarwinds.joboe.logging.LoggerFactory;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.ResourceAttributes;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class ResourceCustomizer implements BiFunction<Resource, ConfigProperties, Resource> {
  private static Resource resource;

  @Override
  public Resource apply(Resource resource, ConfigProperties configProperties) {
    ResourceBuilder resourceBuilder =
        resource.toBuilder()
            .put("sw.data.module", "apm")
            .put("sw.apm.version", BuildConfig.SOLARWINDS_AGENT_VERSION);
    String resourceAttribute = resource.getAttribute(ResourceAttributes.PROCESS_COMMAND_LINE);
    List<String> processArgs = resource.getAttribute(ResourceAttributes.PROCESS_COMMAND_ARGS);

    if (resourceAttribute != null) {
      resourceBuilder.put(
          ResourceAttributes.PROCESS_COMMAND_LINE,
          resourceAttribute.replaceAll("(sw.apm.service.key=)\\S+", "$1****"));
    }

    if (processArgs != null) {
      List<String> args =
          processArgs.stream()
              .map(arg -> arg.replaceAll("(sw.apm.service.key=)\\S+", "$1****"))
              .collect(Collectors.toList());
      resourceBuilder.put(ResourceAttributes.PROCESS_COMMAND_ARGS, args);
    }

    ResourceCustomizer.resource = resourceBuilder.build();
    LoggerFactory.getLogger()
        .debug(
            String.format(
                "This log line is used for validation only: service.name: %s",
                resource.getAttribute(ResourceAttributes.SERVICE_NAME)));
    return ResourceCustomizer.resource;
  }

  public static Resource getResource() {
    return resource;
  }
}
