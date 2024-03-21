package com.solarwinds.opentelemetry.extensions.initialize;

import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.opentelemetry.extensions.initialize.config.BuildConfig;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.ResourceAttributes;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class AutoConfiguredResourceCustomizer
    implements BiFunction<Resource, ConfigProperties, Resource> {
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

    AutoConfiguredResourceCustomizer.resource = resourceBuilder.build();
    LoggerFactory.getLogger()
        .debug(
            String.format(
                "This log line is used for validation only: service.name: %s",
                resource.getAttribute(ResourceAttributes.SERVICE_NAME)));
    return AutoConfiguredResourceCustomizer.resource;
  }

  public static Resource getResource() {
    return resource;
  }
}
