package com.solarwinds.opentelemetry.extensions.config.provider;

import static com.solarwinds.opentelemetry.extensions.ApmResourceProvider.moduleKey;
import static com.solarwinds.opentelemetry.extensions.ApmResourceProvider.versionKey;

import com.google.auto.service.AutoService;
import com.solarwinds.opentelemetry.extensions.BuildConfig;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.resources.Resource;
import lombok.Getter;

@AutoService(ComponentProvider.class)
public class ResourceComponentProvider implements ComponentProvider<Resource> {

  public static final String COMPONENT_NAME = "swo/resource";

  @Getter private static Resource resource = null;

  @Override
  public Class<Resource> getType() {
    return Resource.class;
  }

  @Override
  public String getName() {
    return COMPONENT_NAME;
  }

  @Override
  public Resource create(DeclarativeConfigProperties declarativeConfigProperties) {
    Attributes resourceAttributes =
        Attributes.of(moduleKey, "apm", versionKey, BuildConfig.SOLARWINDS_AGENT_VERSION);

    resource = Resource.create(resourceAttributes);
    return resource;
  }
}
