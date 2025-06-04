package com.solarwinds.opentelemetry.extensions;

import com.solarwinds.opentelemetry.extensions.provider.ResourceComponentProvider;
import io.opentelemetry.sdk.resources.Resource;

public final class ResourceArbiter {
  private ResourceArbiter() {}

  public static Resource resource() {
    Resource resource = ResourceCustomizer.getResource();
    return resource == null ? ResourceComponentProvider.getResource() : resource;
  }
}
