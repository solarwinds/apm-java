package com.appoptics.opentelemetry.extensions.initialize;

import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.util.ServiceKeyUtils;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.ResourceAttributes;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.appoptics.opentelemetry.extensions.initialize.AppOpticsConfigurationLoader.mergeEnvWithSysProperties;

public class AutoConfiguredResourceCustomizer implements BiFunction<Resource, ConfigProperties, Resource> {
    private static Resource resource;

    @Override
    public Resource apply(Resource resource, ConfigProperties configProperties) {
        String serviceName = resource.getAttribute(ResourceAttributes.SERVICE_NAME);
        ResourceBuilder resourceBuilder = resource.toBuilder();

        if (isServiceNameNullOrUndefined(serviceName)) {
            Map<String, String> configs = mergeEnvWithSysProperties(System.getenv(), System.getProperties());
            String serviceKey = configs.get(ConfigProperty.AGENT_SERVICE_KEY.getEnvironmentVariableKey());
            if (serviceKey != null) {
                String name = ServiceKeyUtils.getServiceName(serviceKey);
                resourceBuilder.put(ResourceAttributes.SERVICE_NAME, name);
            }

        } else {
            String serviceKey = (String) ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY);
            if (serviceKey != null) {
                try {
                    String key = String.format("%s:%s", ServiceKeyUtils.getApiKey(serviceKey), serviceName);
                    ConfigManager.setConfig(ConfigProperty.AGENT_SERVICE_KEY, key);
                } catch (InvalidConfigException e) {
                    LoggerFactory.getLogger()
                            .warn(
                                    String.format("[AutoConfiguredResourceCustomizer] Unable to update service name to %s", serviceName)
                            );
                }
            }
        }

        String resourceAttribute = resource.getAttribute(ResourceAttributes.PROCESS_COMMAND_LINE);
        List<String> processArgs = resource.getAttribute(ResourceAttributes.PROCESS_COMMAND_ARGS);
        if (resourceAttribute != null) {
            resourceBuilder.put(ResourceAttributes.PROCESS_COMMAND_LINE, resourceAttribute.replaceAll("(sw.apm.service.key=)\\S+", "$1****"));
        }

        if (processArgs != null) {
            List<String> args = processArgs.stream().map(
                    arg -> arg.replaceAll("(sw.apm.service.key=)\\S+", "$1****")
            ).collect(Collectors.toList());
            resourceBuilder.put(ResourceAttributes.PROCESS_COMMAND_ARGS, args);
        }

        AutoConfiguredResourceCustomizer.resource = resourceBuilder.build();
        return AutoConfiguredResourceCustomizer.resource;
    }

    private boolean isServiceNameNullOrUndefined(String serviceName){
        return serviceName == null || serviceName.matches("unknown.*");
    }

    public static Resource getResource() {
        return resource;
    }
}
