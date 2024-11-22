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

@AutoService(ResourceProvider.class)
public class HostIdResourceProvider implements ResourceProvider {

  @Override
  public Resource createResource(ConfigProperties configProperties) {
    AttributesBuilder builder = Attributes.builder();

    HostId hostId = ServerHostInfoReader.INSTANCE.getHostId();
    builder.put(ResourceAttributes.CONTAINER_ID, hostId.getDockerContainerId());
    builder.put(ResourceAttributes.PROCESS_PID, (long) hostId.getPid());
    builder.put(AttributeKey.stringArrayKey("mac.addresses"), hostId.getMacAddresses());

    builder.put(
        AttributeKey.stringKey("azure.app.service.instance.id"),
        hostId.getAzureAppServiceInstanceId());
    builder.put(ResourceAttributes.HOST_ID, hostId.getHerokuDynoId());
    builder.put(AttributeKey.stringKey("sw.uams.client.id"), hostId.getUamsClientId());
    builder.put(AttributeKey.stringKey("uuid"), hostId.getUuid());

    HostId.K8sMetadata k8sMetadata = hostId.getK8sMetadata();
    if (k8sMetadata != null) {
      builder.put(ResourceAttributes.K8S_POD_UID, k8sMetadata.getPodUid());
      builder.put(ResourceAttributes.K8S_NAMESPACE_NAME, k8sMetadata.getNamespace());
      builder.put(ResourceAttributes.K8S_POD_NAME, k8sMetadata.getPodName());
    }

    HostId.AwsMetadata awsMetadata = hostId.getAwsMetadata();
    if (awsMetadata != null) {
      builder.put(ResourceAttributes.HOST_ID, awsMetadata.getHostId());
      builder.put(ResourceAttributes.HOST_NAME, awsMetadata.getHostName());
      builder.put(ResourceAttributes.CLOUD_PROVIDER, awsMetadata.getCloudProvider());

      builder.put(ResourceAttributes.CLOUD_ACCOUNT_ID, awsMetadata.getCloudAccountId());
      builder.put(ResourceAttributes.CLOUD_PLATFORM, awsMetadata.getCloudPlatform());
      builder.put(
          ResourceAttributes.CLOUD_AVAILABILITY_ZONE, awsMetadata.getCloudAvailabilityZone());

      builder.put(ResourceAttributes.CLOUD_REGION, awsMetadata.getCloudRegion());
      builder.put(ResourceAttributes.HOST_IMAGE_ID, awsMetadata.getHostImageId());
      builder.put(ResourceAttributes.HOST_TYPE, awsMetadata.getHostType());
    }

    HostId.AzureVmMetadata azureVmMetadata = hostId.getAzureVmMetadata();
    if (azureVmMetadata != null) {
      builder.put(ResourceAttributes.HOST_ID, azureVmMetadata.getHostId());
      builder.put(ResourceAttributes.HOST_NAME, azureVmMetadata.getHostName());
      builder.put(ResourceAttributes.CLOUD_PROVIDER, azureVmMetadata.getCloudProvider());

      builder.put(ResourceAttributes.CLOUD_ACCOUNT_ID, azureVmMetadata.getCloudAccountId());
      builder.put(ResourceAttributes.CLOUD_PLATFORM, azureVmMetadata.getCloudPlatform());
      builder.put(ResourceAttributes.CLOUD_REGION, azureVmMetadata.getCloudRegion());

      builder.put(AttributeKey.stringKey("azure.vm.name"), azureVmMetadata.getAzureVmName());
      builder.put(AttributeKey.stringKey("azure.vm.size"), azureVmMetadata.getAzureVmSize());
      builder.put(
          AttributeKey.stringKey("azure.resourcegroup.name"),
          azureVmMetadata.getAzureResourceGroupName());

      builder.put(
          AttributeKey.stringKey("azure.vm.scaleset.name"),
          azureVmMetadata.getAzureVmScaleSetName());
    }

    builder.put(ResourceAttributes.HOST_NAME, hostId.getHostname());
    builder.put(ResourceAttributes.CLOUD_AVAILABILITY_ZONE, hostId.getEc2AvailabilityZone());
    builder.put(ResourceAttributes.HOST_ID, hostId.getEc2InstanceId());
    return Resource.create(builder.build());
  }
}
