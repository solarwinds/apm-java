package com.solarwinds.opentelemetry.extensions.config.provider;

import com.google.auto.service.AutoService;
import com.solarwinds.opentelemetry.extensions.config.HostIdResourceUtil;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.resources.Resource;

@AutoService(ComponentProvider.class)
public class HostIdResourceComponentProvider implements ComponentProvider<Resource> {
  @Override
  public Class<Resource> getType() {
    return Resource.class;
  }

  @Override
  public String getName() {
    return "swo/hostIdResource";
  }

  @Override
  public Resource create(DeclarativeConfigProperties declarativeConfigProperties) {
    return Resource.create(HostIdResourceUtil.createAttribute());
  }
}
