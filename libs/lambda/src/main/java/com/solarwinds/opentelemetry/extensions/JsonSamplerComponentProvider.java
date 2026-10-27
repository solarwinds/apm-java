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

package com.solarwinds.opentelemetry.extensions;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.sampling.SettingsFetcher;
import com.solarwinds.opentelemetry.extensions.config.provider.AbstractSamplerComponentProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;

@AutoService(ComponentProvider.class)
public class JsonSamplerComponentProvider extends AbstractSamplerComponentProvider {

  public static final String DEFAULT_SETTINGS_FILE = "/tmp/solarwinds-apm-settings.json";

  private static final String FILE = "file";

  @Override
  public String getName() {
    return SharedNames.JSON_SAMPLER_NAME;
  }

  @Override
  protected SettingsFetcher createSettingsFetcher(DeclarativeConfigProperties properties)
      throws InvalidConfigException {

    String file = properties.getString(FILE, DEFAULT_SETTINGS_FILE);
    if (file.trim().isEmpty()) {
      throw new InvalidConfigException(
          "Invalid sampler " + FILE + " config, value cannot be empty");
    }

    return new AwsLambdaSettingsFetcher(new FileSettingsReader(file));
  }
}
