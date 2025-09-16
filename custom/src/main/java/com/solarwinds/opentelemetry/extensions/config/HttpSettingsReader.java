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

package com.solarwinds.opentelemetry.extensions.config;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.ServiceKeyUtils;
import com.solarwinds.joboe.core.settings.SettingsReader;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.opentelemetry.extensions.ResourceArbiter;
import io.opentelemetry.semconv.incubating.HostIncubatingAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpSettingsReader implements SettingsReader {

  private final Pattern regex = Pattern.compile("^https?://(.*)");

  private static final Logger logger = LoggerFactory.getLogger();

  private final HttpSettingsReaderDelegate delegate;

  public HttpSettingsReader(HttpSettingsReaderDelegate delegate) {
    this.delegate = delegate;
  }

  @Override
  public Settings getSettings() {
    String collectorUrl =
        constructSettingsEndpoint(
            ConfigManager.getConfigOptional(
                ConfigProperty.AGENT_COLLECTOR, "apm.collector.na-01.cloud.solarwinds.com"));
    String serviceKey =
        ConfigManager.getConfigOptional(ConfigProperty.AGENT_SERVICE_KEY, ":unknown-java");
    String serviceName = ServiceKeyUtils.getServiceName(serviceKey);

    String apiToken = ServiceKeyUtils.getApiKey(serviceKey);
    String hostname = ResourceArbiter.resource().getAttribute(HostIncubatingAttributes.HOST_NAME);
    String settingsUrl = String.format("%s/v1/settings/%s/%s", collectorUrl, serviceName, hostname);

    String tokenHeader = String.format("Bearer %s", apiToken);
    Settings fetchedSettings = delegate.fetchSettings(settingsUrl, tokenHeader);
    logger.debug(String.format("Got settings from http: %s", fetchedSettings));

    return fetchedSettings;
  }

  @Override
  public void close() {}

  private String constructSettingsEndpoint(String collectorUrl) {
    Matcher matcher = regex.matcher(collectorUrl);
    if (matcher.find()) {
      return String.format("https://%s", matcher.group(1));
    }
    return String.format("https://%s", collectorUrl);
  }
}
