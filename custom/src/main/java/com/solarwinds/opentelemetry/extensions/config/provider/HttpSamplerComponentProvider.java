/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
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

package com.solarwinds.opentelemetry.extensions.config.provider;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.config.ProxyConfig;
import com.solarwinds.joboe.config.ServiceKeyUtils;
import com.solarwinds.joboe.sampling.SamplingConfiguration;
import com.solarwinds.joboe.sampling.SettingsFetcher;
import com.solarwinds.opentelemetry.extensions.SharedNames;
import com.solarwinds.opentelemetry.extensions.config.HttpSettingsFetcher;
import com.solarwinds.opentelemetry.extensions.config.HttpSettingsReader;
import com.solarwinds.opentelemetry.extensions.config.HttpSettingsReaderDelegate;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import java.util.function.Function;

@AutoService(ComponentProvider.class)
public class HttpSamplerComponentProvider extends AbstractSamplerComponentProvider {

  private static final String COLLECTOR = "collector";

  private static final String SERVICE_KEY = "service_key";

  private static final String CONTEXT_TTL = "context_ttl";

  private static final String CONTEXT_MAX_EVENTS = "context_max_events";

  private static final String CONTEXT_MAX_BACKTRACES = "context_max_backtraces";

  private static final String PROXY = "proxy";

  private static final String PROXY_HOST = "host";

  private static final String PROXY_PORT = "port";

  private static final long POLLING_INTERVAL_SECONDS = 60;

  private final Function<ProxyConfig, HttpSettingsReaderDelegate> delegateFactory;

  public HttpSamplerComponentProvider() {
    this(HttpSettingsReaderDelegate::new);
  }

  HttpSamplerComponentProvider(Function<ProxyConfig, HttpSettingsReaderDelegate> delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public String getName() {
    return SharedNames.HTTP_SAMPLER_NAME;
  }

  @Override
  protected void parseConfig(
      DeclarativeConfigProperties properties,
      SamplingConfiguration.SamplingConfigurationBuilder builder)
      throws InvalidConfigException {

    Integer ttl = parsePositiveInt(properties, CONTEXT_TTL);
    if (ttl != null) {
      builder.ttl(ttl);
    }

    Integer maxEvents = parsePositiveInt(properties, CONTEXT_MAX_EVENTS);
    if (maxEvents != null) {
      builder.maxEvents(maxEvents);
    }

    Integer maxBacktraces = parsePositiveInt(properties, CONTEXT_MAX_BACKTRACES);
    if (maxBacktraces != null) {
      builder.maxBacktraces(maxBacktraces);
    }
  }

  @Override
  protected SettingsFetcher createSettingsFetcher(DeclarativeConfigProperties properties)
      throws InvalidConfigException {

    String collector = parseNonEmpty(properties, COLLECTOR);
    String serviceKey = parseNonEmpty(properties, SERVICE_KEY);
    if (serviceKey == null) {
      throw new InvalidConfigException("Missing required sampler " + SERVICE_KEY + " config");
    }

    serviceKey = ServiceKeyUtils.transformServiceKey(serviceKey);
    return new HttpSettingsFetcher(
        HttpSettingsReader.forFileConfig(
            delegateFactory.apply(parseProxy(properties)), collector, serviceKey),
        POLLING_INTERVAL_SECONDS);
  }

  private static Integer parsePositiveInt(DeclarativeConfigProperties properties, String key)
      throws InvalidConfigException {
    Integer value = properties.getInt(key);
    if (value != null && value < 1) {
      throw new InvalidConfigException(
          "Invalid sampler " + key + " config, value must be a positive integer");
    }
    return value;
  }

  private static String parseNonEmpty(DeclarativeConfigProperties properties, String key)
      throws InvalidConfigException {
    String value = properties.getString(key);
    if (value != null && value.trim().isEmpty()) {
      throw new InvalidConfigException("Invalid sampler " + key + " config, value cannot be empty");
    }
    return value;
  }

  private ProxyConfig parseProxy(DeclarativeConfigProperties properties)
      throws InvalidConfigException {
    DeclarativeConfigProperties proxy = properties.getStructured(PROXY);
    if (proxy == null) {
      return null;
    }

    String host = proxy.getString(PROXY_HOST);
    Integer port = proxy.getInt(PROXY_PORT);
    if (host == null || host.isEmpty() || port == null || port < 1 || port > 65535) {
      throw new InvalidConfigException(
          "Invalid sampler proxy config, both proxy.host and a valid proxy.port are required");
    }
    return new ProxyConfig(host, port, null, null);
  }
}
