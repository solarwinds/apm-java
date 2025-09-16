package com.solarwinds.opentelemetry.extensions.config;

import static com.solarwinds.opentelemetry.extensions.SharedNames.SW_OTEL_PROXY_HOST_KEY;
import static com.solarwinds.opentelemetry.extensions.SharedNames.SW_OTEL_PROXY_PORT_KEY;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

public final class ProxyHelper {
  private ProxyHelper() {}

  public static boolean isProxyConfigured(ConfigProperties properties) {
    return properties.getString(SW_OTEL_PROXY_HOST_KEY) != null
        && properties.getInt(SW_OTEL_PROXY_PORT_KEY) != null;
  }
}
