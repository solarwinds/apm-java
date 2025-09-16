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
import com.solarwinds.opentelemetry.extensions.SolarwindsSampler;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class SamplerComponentProvider implements ComponentProvider<Sampler> {

  public static final String COMPONENT_NAME = "swo/sampler";

  @Override
  public Class<Sampler> getType() {
    return Sampler.class;
  }

  @Override
  public String getName() {
    return COMPONENT_NAME;
  }

  @Override
  public Sampler create(
      io.opentelemetry.api.incubator.config.DeclarativeConfigProperties
          declarativeConfigProperties) {
    return new SolarwindsSampler();
  }
}
