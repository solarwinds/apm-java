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

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ExtendedDeclarativeConfigProperties;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

final class ExporterConfigs {

  private ExporterConfigs() {}

  static DeclarativeConfigProperties exporterConfig(String yaml) {
    DeclarativeConfigProperties exporter =
        DeclarativeConfiguration.toConfigProperties(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
            .getStructured("swo_otlp");

    ExtendedDeclarativeConfigProperties config =
        mock(ExtendedDeclarativeConfigProperties.class, delegatesTo(exporter));
    doReturn(ConfigProvider.noop()).when(config).getConfigProvider();
    return config;
  }
}
