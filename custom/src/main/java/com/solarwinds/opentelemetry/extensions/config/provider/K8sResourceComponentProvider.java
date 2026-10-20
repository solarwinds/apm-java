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
import com.solarwinds.opentelemetry.extensions.config.HostIdResourceUtil;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.K8sAttributes;

@AutoService(ComponentProvider.class)
public class K8sResourceComponentProvider implements ComponentProvider {

  public static final String COMPONENT_NAME = "swo_k8s";

  @Override
  public Class<Resource> getType() {
    return Resource.class;
  }

  @Override
  public String getName() {
    return COMPONENT_NAME;
  }

  @Override
  public Resource create(DeclarativeConfigProperties config) {
    AttributesBuilder builder = HostIdResourceUtil.createK8sAttributes().toBuilder();
    putIfConfigured(builder, K8sAttributes.K8S_NAMESPACE_NAME, config.getString("namespace"));
    putIfConfigured(builder, K8sAttributes.K8S_POD_UID, config.getString("pod_uid"));
    putIfConfigured(builder, K8sAttributes.K8S_POD_NAME, config.getString("pod_name"));
    return Resource.create(builder.build());
  }

  private static void putIfConfigured(
      AttributesBuilder builder, AttributeKey<String> key, String value) {
    if (value != null && !value.isEmpty()) {
      builder.put(key, value);
    }
  }
}
