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

import static com.solarwinds.opentelemetry.extensions.config.provider.AutoConfigurationCustomizerProviderImpl.isAgentEnabled;

import com.solarwinds.joboe.config.JavaRuntimeVersionChecker;
import com.solarwinds.joboe.sampling.SettingsFetcher;
import com.solarwinds.opentelemetry.extensions.config.HttpSettingsFetcher;
import com.solarwinds.opentelemetry.extensions.config.HttpSettingsReader;
import com.solarwinds.opentelemetry.extensions.config.HttpSettingsReaderDelegate;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class SolarwindsTracerProviderCustomizer
    implements BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder> {

  private static final long POLLING_INTERVAL_SECONDS = 60;

  private final Supplier<SettingsFetcher> settingsFetcherFactory;

  public SolarwindsTracerProviderCustomizer() {
    this(
        () ->
            new HttpSettingsFetcher(
                HttpSettingsReader.forSystemConfig(new HttpSettingsReaderDelegate()),
                POLLING_INTERVAL_SECONDS));
  }

  SolarwindsTracerProviderCustomizer(Supplier<SettingsFetcher> settingsFetcherFactory) {
    this.settingsFetcherFactory = settingsFetcherFactory;
  }

  @Override
  public SdkTracerProviderBuilder apply(
      SdkTracerProviderBuilder tracerProvider, ConfigProperties config) {
    if (isAgentEnabled()) {
      if (JavaRuntimeVersionChecker.isJdkVersionSupported()) {
        tracerProvider.addSpanProcessor(new SolarwindsProfilingSpanProcessor());
      }

      tracerProvider
          .setSampler(
              new SolarwindsSampler(
                  settingsFetcherFactory.get(), SamplingConfigProvider.getSamplingConfiguration()))
          .addSpanProcessor(new ResponseTimeSpanProcessor());
    }

    return tracerProvider;
  }
}
