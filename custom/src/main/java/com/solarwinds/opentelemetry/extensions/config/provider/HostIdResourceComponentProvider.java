package com.solarwinds.opentelemetry.extensions.config.provider;

import com.google.auto.service.AutoService;
import com.solarwinds.opentelemetry.extensions.config.HostIdResourceUtil;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.resources.Resource;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class HostIdResourceComponentProvider implements ComponentProvider<Resource> {

  public static final String COMPONENT_NAME = "swo/hostIdResource";

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
    Attributes attribute = HostIdResourceUtil.createAttribute();
    ResourceComponentProvider.setResource(
        ResourceComponentProvider.getResource().merge(Resource.create(attribute)));
    return Resource.create(attribute);
  }
}
