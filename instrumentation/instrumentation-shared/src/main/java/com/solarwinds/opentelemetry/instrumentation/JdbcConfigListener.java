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

package com.solarwinds.opentelemetry.instrumentation;

import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.api.incubator.config.internal.DeclarativeConfigUtil;
import io.opentelemetry.javaagent.tooling.BeforeAgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

/**
 * Reads the {@code swo_jdbc} node under {@code instrumentation/development.java} before any
 * instrumentation is installed, since the jdbc and hibernate modules consult the SQL settings at
 * install time.
 */
@AutoService(BeforeAgentListener.class)
public class JdbcConfigListener implements BeforeAgentListener {
  static final String INSTRUMENTATION_NAME = "swo_jdbc";

  @Override
  public void beforeAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    JdbcConfig.apply(
        DeclarativeConfigUtil.getInstrumentationConfig(
            autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk(), INSTRUMENTATION_NAME));
  }
}
