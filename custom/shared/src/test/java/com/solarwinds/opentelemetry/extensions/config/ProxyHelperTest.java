package com.solarwinds.opentelemetry.extensions.config;

import static com.solarwinds.opentelemetry.extensions.SharedNames.SW_OTEL_PROXY_HOST_KEY;
import static com.solarwinds.opentelemetry.extensions.SharedNames.SW_OTEL_PROXY_PORT_KEY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProxyHelperTest {

  @Mock private ConfigProperties configProperties;

  @Test
  void shouldReturnTrueWhenProxyConfigured() {
    when(configProperties.getString(eq(SW_OTEL_PROXY_HOST_KEY))).thenReturn("localhost");
    when(configProperties.getInt(eq(SW_OTEL_PROXY_PORT_KEY))).thenReturn(8080);

    assertTrue(ProxyHelper.isProxyConfigured(configProperties));
  }

  @Test
  void shouldReturnFalseWhenProxyHostMissing() {
    when(configProperties.getString(eq(SW_OTEL_PROXY_HOST_KEY))).thenReturn(null);
    assertFalse(ProxyHelper.isProxyConfigured(configProperties));
  }

  @Test
  void shouldReturnFalseWhenProxyPortMissing() {
    when(configProperties.getString(eq(SW_OTEL_PROXY_HOST_KEY))).thenReturn("localhost");
    when(configProperties.getInt(eq(SW_OTEL_PROXY_PORT_KEY))).thenReturn(null);

    assertFalse(ProxyHelper.isProxyConfigured(configProperties));
  }
}
