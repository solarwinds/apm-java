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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.K8sAttributes;
import org.junit.jupiter.api.Test;

class K8sResourceComponentProviderTest {
  private final K8sResourceComponentProvider tested = new K8sResourceComponentProvider();

  @Test
  void testName() {
    assertEquals("swo_k8s", tested.getName());
  }

  @Test
  void configuredValuesAreUsed() {
    DeclarativeConfigProperties config = mock(DeclarativeConfigProperties.class);
    when(config.getString("namespace")).thenReturn("ns");
    when(config.getString("pod_uid")).thenReturn("uid");
    when(config.getString("pod_name")).thenReturn("name");

    Resource resource = tested.create(config);

    assertEquals("ns", resource.getAttribute(K8sAttributes.K8S_NAMESPACE_NAME));
    assertEquals("uid", resource.getAttribute(K8sAttributes.K8S_POD_UID));
    assertEquals("name", resource.getAttribute(K8sAttributes.K8S_POD_NAME));
  }

  @Test
  void onlyConfiguredValuesAreSetWhenPartial() {
    DeclarativeConfigProperties config = mock(DeclarativeConfigProperties.class);
    when(config.getString("pod_name")).thenReturn("name");

    Resource resource = tested.create(config);
    assertEquals("name", resource.getAttribute(K8sAttributes.K8S_POD_NAME));
    assertEquals(1, resource.getAttributes().size());
  }
}
