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
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;

@AutoService(ResourceProvider.class)
public class ApmResourceProvider implements ResourceProvider {
  public static final AttributeKey<String> moduleKey = AttributeKey.stringKey("sw.data.module");

  public static final AttributeKey<String> versionKey = AttributeKey.stringKey("sw.apm.version");

  @Override
  public Resource createResource(ConfigProperties configProperties) {
    Attributes resourceAttributes =
        Attributes.of(moduleKey, "apm", versionKey, BuildConfig.SOLARWINDS_AGENT_VERSION);
    return Resource.create(resourceAttributes);
  }
}
