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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.solarwinds.joboe.core.HostId;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.semconv.K8sAttributes;
import org.junit.jupiter.api.Test;

class HostIdResourceUtilTest {
  @Test
  void uamsAttributesDoNotIncludeDroppedKeys() {
    Attributes attributes = HostIdResourceUtil.createUamsAttributes();
    assertNull(attributes.get(AttributeKey.stringArrayKey("mac.addresses")));
    assertNull(attributes.get(AttributeKey.stringKey("host.id")));
  }

  @Test
  void k8sAttributesOnlyContainK8sKeys() {
    HostIdResourceUtil.createK8sAttributes()
        .forEach((key, value) -> assertTrue(key.getKey().startsWith("k8s.")));
  }

  @Test
  void uamsAttributesContainClientId() {
    HostId hostId = HostId.builder().uamsClientId("client-id").herokuDynoId("dyno").build();
    Attributes attributes = HostIdResourceUtil.createUamsAttributes(hostId);

    assertEquals("client-id", attributes.get(AttributeKey.stringKey("sw.uams.client.id")));
    assertEquals(1, attributes.size());
  }

  @Test
  void k8sAttributesAreMappedFromMetadata() {
    HostId hostId =
        HostId.builder()
            .k8sMetadata(
                HostId.K8sMetadata.builder().namespace("ns").podName("pod").podUid("uid").build())
            .build();
    Attributes attributes = HostIdResourceUtil.createK8sAttributes(hostId);

    assertEquals("ns", attributes.get(K8sAttributes.K8S_NAMESPACE_NAME));
    assertEquals("pod", attributes.get(K8sAttributes.K8S_POD_NAME));
    assertEquals("uid", attributes.get(K8sAttributes.K8S_POD_UID));
  }

  @Test
  void k8sAttributesEmptyWithoutMetadata() {
    assertEquals(0, HostIdResourceUtil.createK8sAttributes(HostId.builder().build()).size());
  }
}
