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

package com.solarwinds.joboe.core;

import static com.solarwinds.joboe.core.util.ServerHostInfoReader.setIfNotNull;

import com.solarwinds.joboe.core.rpc.HostType;
import com.solarwinds.trace.ingestion.proto.Collector;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A combination of values to identify a host that makes the RPC call
 *
 * @author pluk
 */
@Data
@Builder
public class HostId {
  private final String hostname;
  private final int pid;
  @Builder.Default private List<String> macAddresses = Collections.emptyList();
  private final String ec2InstanceId;
  private final String ec2AvailabilityZone;
  private final String dockerContainerId;
  private final String herokuDynoId;
  private final String azureAppServiceInstanceId;
  @Builder.Default private final HostType hostType = HostType.PERSISTENT;
  private final String uamsClientId;
  private final String uuid;
  private final AwsMetadata awsMetadata;
  private final AzureVmMetadata azureVmMetadata;
  private final K8sMetadata k8sMetadata;

  @Value
  @Builder
  public static class AzureVmMetadata {
    @Builder.Default String cloudProvider = "azure";
    @Builder.Default String cloudPlatform = "azure_vm";
    String cloudRegion;
    String cloudAccountId;
    String hostId;
    String hostName;
    String azureVmName;
    String azureVmSize;
    String azureVmScaleSetName;
    String azureResourceGroupName;

    public static AzureVmMetadata fromJson(String payload) throws JSONException {
      JSONObject jsonObject = new JSONObject(payload);
      JSONObject compute = jsonObject.getJSONObject("compute");
      return AzureVmMetadata.builder()
          .hostId(compute.getString("vmId"))
          .cloudRegion(compute.getString("location"))
          .hostName(compute.getString("name"))
          .azureVmName(compute.getString("name"))
          .cloudAccountId(compute.getString("subscriptionId"))
          .azureVmSize(compute.getString("vmSize"))
          .azureVmScaleSetName(compute.getString("vmScaleSetName"))
          .azureResourceGroupName(compute.getString("resourceGroupName"))
          .build();
    }

    public Collector.Azure toGrpc() {
      return Collector.Azure.newBuilder()
          .setCloudProvider(cloudProvider)
          .setCloudPlatform(cloudPlatform)
          .setCloudRegion(cloudRegion)
          .setCloudAccountId(cloudAccountId)
          .setHostId(hostId)
          .setHostName(hostName)
          .setAzureVmName(azureVmName)
          .setAzureVmSize(azureVmSize)
          .setAzureVmScaleSetName(azureVmScaleSetName)
          .setAzureResourceGroupName(azureResourceGroupName)
          .build();
    }
  }

  @Value
  @Builder
  public static class AwsMetadata {
    @Builder.Default String cloudProvider = "aws";
    @Builder.Default String cloudPlatform = "aws_ec2";
    String cloudAccountId;
    String cloudRegion;
    String cloudAvailabilityZone;
    String hostId;
    String hostImageId;
    String hostName;
    String hostType;

    public static AwsMetadata fromJson(String json, String hostName) throws JSONException {
      if (json == null) return null;
      JSONObject jsonObject = new JSONObject(json);
      return AwsMetadata.builder()
          .cloudRegion(jsonObject.getString("region"))
          .cloudAccountId(jsonObject.getString("accountId"))
          .cloudAvailabilityZone(jsonObject.getString("availabilityZone"))
          .hostId(jsonObject.getString("instanceId"))
          .hostImageId(jsonObject.getString("imageId"))
          .hostType(jsonObject.getString("instanceType"))
          .hostName(hostName)
          .build();
    }

    public Collector.Aws toGrpc() {
      return Collector.Aws.newBuilder()
          .setCloudProvider(cloudProvider)
          .setCloudPlatform(cloudPlatform)
          .setCloudAccountId(cloudAccountId)
          .setCloudRegion(cloudRegion)
          .setCloudAvailabilityZone(cloudAvailabilityZone)
          .setHostId(hostId)
          .setHostImageId(hostImageId)
          .setHostName(hostName)
          .setHostType(hostType)
          .build();
    }
  }

  @Value
  @Builder
  public static class K8sMetadata {
    String namespace;
    String podName;
    String podUid;

    public Collector.K8s toGrpc() {
      Collector.K8s.Builder builder = Collector.K8s.newBuilder();
      setIfNotNull(builder::setNamespace, namespace);

      setIfNotNull(builder::setPodName, podName);
      setIfNotNull(builder::setPodUid, podUid);
      return builder.build();
    }
  }
}
