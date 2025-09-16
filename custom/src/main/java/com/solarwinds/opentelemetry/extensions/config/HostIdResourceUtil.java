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
import io.opentelemetry.semconv.incubating.CloudIncubatingAttributes;
import io.opentelemetry.semconv.incubating.ContainerIncubatingAttributes;
import io.opentelemetry.semconv.incubating.HostIncubatingAttributes;
import io.opentelemetry.semconv.incubating.K8sIncubatingAttributes;
import io.opentelemetry.semconv.incubating.ProcessIncubatingAttributes;
import io.opentelemetry.semconv.incubating.ServiceIncubatingAttributes;

public final class HostIdResourceUtil {
  private HostIdResourceUtil() {}

  public static Attributes createAttribute() {
    AttributesBuilder builder = Attributes.builder();

    HostId hostId = ServerHostInfoReader.INSTANCE.getHostId();
    builder.put(ContainerIncubatingAttributes.CONTAINER_ID, hostId.getDockerContainerId());
    builder.put(ProcessIncubatingAttributes.PROCESS_PID, (long) hostId.getPid());
    builder.put(AttributeKey.stringArrayKey("mac.addresses"), hostId.getMacAddresses());

    builder.put(
        AttributeKey.stringKey("azure.app.service.instance.id"),
        hostId.getAzureAppServiceInstanceId());
    builder.put(HostIncubatingAttributes.HOST_ID, hostId.getHerokuDynoId());
    builder.put(AttributeKey.stringKey("sw.uams.client.id"), hostId.getUamsClientId());
    builder.put(AttributeKey.stringKey("uuid"), hostId.getUuid());
    builder.put(ServiceIncubatingAttributes.SERVICE_INSTANCE_ID, hostId.getUuid());

    HostId.K8sMetadata k8sMetadata = hostId.getK8sMetadata();
    if (k8sMetadata != null) {
      builder.put(K8sIncubatingAttributes.K8S_POD_UID, k8sMetadata.getPodUid());
      builder.put(K8sIncubatingAttributes.K8S_NAMESPACE_NAME, k8sMetadata.getNamespace());
      builder.put(K8sIncubatingAttributes.K8S_POD_NAME, k8sMetadata.getPodName());
    }

    HostId.AwsMetadata awsMetadata = hostId.getAwsMetadata();
    if (awsMetadata != null) {
      builder.put(HostIncubatingAttributes.HOST_ID, awsMetadata.getHostId());
      builder.put(HostIncubatingAttributes.HOST_NAME, awsMetadata.getHostName());
      builder.put(CloudIncubatingAttributes.CLOUD_PROVIDER, awsMetadata.getCloudProvider());

      builder.put(CloudIncubatingAttributes.CLOUD_ACCOUNT_ID, awsMetadata.getCloudAccountId());
      builder.put(CloudIncubatingAttributes.CLOUD_PLATFORM, awsMetadata.getCloudPlatform());
      builder.put(
          CloudIncubatingAttributes.CLOUD_AVAILABILITY_ZONE,
          awsMetadata.getCloudAvailabilityZone());

      builder.put(CloudIncubatingAttributes.CLOUD_REGION, awsMetadata.getCloudRegion());
      builder.put(HostIncubatingAttributes.HOST_IMAGE_ID, awsMetadata.getHostImageId());
      builder.put(HostIncubatingAttributes.HOST_TYPE, awsMetadata.getHostType());
    }

    HostId.AzureVmMetadata azureVmMetadata = hostId.getAzureVmMetadata();
    if (azureVmMetadata != null) {
      builder.put(HostIncubatingAttributes.HOST_ID, azureVmMetadata.getHostId());
      builder.put(HostIncubatingAttributes.HOST_NAME, azureVmMetadata.getHostName());
      builder.put(CloudIncubatingAttributes.CLOUD_PROVIDER, azureVmMetadata.getCloudProvider());

      builder.put(CloudIncubatingAttributes.CLOUD_ACCOUNT_ID, azureVmMetadata.getCloudAccountId());
      builder.put(CloudIncubatingAttributes.CLOUD_PLATFORM, azureVmMetadata.getCloudPlatform());
      builder.put(CloudIncubatingAttributes.CLOUD_REGION, azureVmMetadata.getCloudRegion());

      builder.put(AttributeKey.stringKey("azure.vm.name"), azureVmMetadata.getAzureVmName());
      builder.put(AttributeKey.stringKey("azure.vm.size"), azureVmMetadata.getAzureVmSize());
      builder.put(
          AttributeKey.stringKey("azure.resourcegroup.name"),
          azureVmMetadata.getAzureResourceGroupName());

      builder.put(
          AttributeKey.stringKey("azure.vm.scaleset.name"),
          azureVmMetadata.getAzureVmScaleSetName());
    }

    builder.put(HostIncubatingAttributes.HOST_NAME, hostId.getHostname());
    builder.put(CloudIncubatingAttributes.CLOUD_AVAILABILITY_ZONE, hostId.getEc2AvailabilityZone());
    builder.put(HostIncubatingAttributes.HOST_ID, hostId.getEc2InstanceId());
    return builder.build();
  }
}
