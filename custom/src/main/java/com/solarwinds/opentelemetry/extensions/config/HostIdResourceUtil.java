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

import com.solarwinds.joboe.core.HostId;
import com.solarwinds.joboe.core.util.ServerHostInfoReader;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.semconv.K8sAttributes;
import io.opentelemetry.semconv.incubating.HostIncubatingAttributes;

public final class HostIdResourceUtil {
  private HostIdResourceUtil() {}

  public static Attributes createAttribute() {
    AttributesBuilder builder = Attributes.builder();
    HostId hostId = ServerHostInfoReader.INSTANCE.getHostId();
    builder.put(AttributeKey.stringArrayKey("mac.addresses"), hostId.getMacAddresses());

    builder.put(HostIncubatingAttributes.HOST_ID, hostId.getHerokuDynoId());
    builder.put(AttributeKey.stringKey("sw.uams.client.id"), hostId.getUamsClientId());
    HostId.K8sMetadata k8sMetadata = hostId.getK8sMetadata();

    if (k8sMetadata != null) {
      builder.put(K8sAttributes.K8S_POD_UID, k8sMetadata.getPodUid());
      builder.put(K8sAttributes.K8S_NAMESPACE_NAME, k8sMetadata.getNamespace());
      builder.put(K8sAttributes.K8S_POD_NAME, k8sMetadata.getPodName());
    }

    return builder.build();
  }
}
