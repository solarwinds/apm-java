package com.solarwinds.opentelemetry.extensions.initialize.config;

import com.solarwinds.joboe.sampling.ResourceMatcher;
import java.util.Set;

class ResourceExtensionsMatcher implements ResourceMatcher {
  private final Set<String> extensions;

  ResourceExtensionsMatcher(Set<String> extensions) {
    super();
    this.extensions = extensions;
  }

  @Override
  public boolean matches(String resource) {
    int queryIndex = resource.indexOf('?');
    resource =
        queryIndex != -1
            ? resource.substring(0, queryIndex)
            : resource; // remove query component if present
    int extensionIndex = resource.lastIndexOf('.');
    String extension =
        extensionIndex != -1 ? resource.substring(extensionIndex + 1).toLowerCase() : null;

    return extension != null && extensions.contains(extension);
  }
}
