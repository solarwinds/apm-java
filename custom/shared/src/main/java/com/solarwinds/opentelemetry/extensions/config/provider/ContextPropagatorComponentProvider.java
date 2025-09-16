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
import com.solarwinds.opentelemetry.extensions.SolarwindsContextPropagator;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class ContextPropagatorComponentProvider implements ComponentProvider<TextMapPropagator> {

  public static final String COMPONENT_NAME = "swo/contextPropagator";

  @Override
  public Class<TextMapPropagator> getType() {
    return TextMapPropagator.class;
  }

  @Override
  public String getName() {
    return COMPONENT_NAME;
  }

  @Override
  public TextMapPropagator create(DeclarativeConfigProperties declarativeConfigProperties) {
    return new SolarwindsContextPropagator();
  }
}
