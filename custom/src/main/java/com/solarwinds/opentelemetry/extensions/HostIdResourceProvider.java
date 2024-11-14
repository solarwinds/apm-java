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
import com.solarwinds.joboe.core.HostId;
import com.solarwinds.joboe.core.util.ServerHostInfoReader;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import java.util.function.BiConsumer;

@AutoService(ResourceProvider.class)
public class HostIdResourceProvider implements ResourceProvider {

  @Override
  public Resource createResource(ConfigProperties configProperties) {
    AttributesBuilder builder = Attributes.builder();

    HostId hostId = ServerHostInfoReader.INSTANCE.getHostId();
    setIfNotNull(builder::put, ResourceAttributes.HOST_NAME, hostId.getHostname());
    setIfNotNull(
        builder::put, ResourceAttributes.CLOUD_AVAILABILITY_ZONE, hostId.getEc2AvailabilityZone());
    setIfNotNull(builder::put, ResourceAttributes.HOST_ID, hostId.getEc2InstanceId());

    setIfNotNull(builder::put, ResourceAttributes.CONTAINER_ID, hostId.getDockerContainerId());
    setIfNotNull(builder::put, ResourceAttributes.PROCESS_PID, (long) hostId.getPid());
    setIfNotNull(
        builder::put, AttributeKey.stringArrayKey("mac.addresses"), hostId.getMacAddresses());

    setIfNotNull(
        builder::put,
        AttributeKey.stringKey("azure.app.service.instance.id"),
        hostId.getAzureAppServiceInstanceId());
    setIfNotNull(builder::put, ResourceAttributes.HOST_ID, hostId.getHerokuDynoId());
    setIfNotNull(
        builder::put, AttributeKey.stringKey("sw.uams.client.id"), hostId.getUamsClientId());
    setIfNotNull(builder::put, AttributeKey.stringKey("uuid"), hostId.getUuid());

    return Resource.create(builder.build());
  }

  private <V> void setIfNotNull(
      BiConsumer<AttributeKey<V>, V> setter, AttributeKey<V> key, V value) {
    if (value != null) {
      setter.accept(key, value);
    }
  }
}
