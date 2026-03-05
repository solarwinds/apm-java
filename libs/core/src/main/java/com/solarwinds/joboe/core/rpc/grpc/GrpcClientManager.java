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

package com.solarwinds.joboe.core.rpc.grpc;

import com.solarwinds.joboe.core.rpc.Client;
import com.solarwinds.joboe.core.rpc.RpcClient;
import com.solarwinds.joboe.core.rpc.RpcClientManager;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;

/**
 * Manages creation of {@link GrpcClient}. Currently, only 2 gRPC client instances are allowed per
 * JVM process - one for Tracing operation and another one for all others
 *
 * @author pluk
 */
public class GrpcClientManager extends RpcClientManager {
  private static final Logger logger = LoggerFactory.getLogger();
  private static Client tracingRpcClient; // one instance for tracing purpose
  private static boolean clientsInitialized = false;
  private static Client nonTracingRpcClient; // one instance for everything else

  public GrpcClientManager() {}

  @Override
  protected Client getClientImpl(OperationType operationType, String serviceKey) {
    synchronized (this) {
      if (!clientsInitialized) {
        initializeClients(serviceKey);
        clientsInitialized = true;
      }
    }

    if (operationType == OperationType.TRACING || operationType == OperationType.PROFILING) {
      return tracingRpcClient;
    } else {
      return nonTracingRpcClient;
    }
  }

  private void initializeClients(String serviceKey) {
    try {
      tracingRpcClient =
          new RpcClient(
              collectorHost,
              collectorPort,
              serviceKey,
              new GrpcClient.GrpcProtocolClientFactory(collectorCertLocation),
              RpcClient.TaskType.POST_EVENTS);
    } catch (Exception e) {
      logger.warn("Failed to initialize rpc tracing client: " + e.getMessage(), e);
    }

    try {
      nonTracingRpcClient =
          new RpcClient(
              collectorHost,
              collectorPort,
              serviceKey,
              new GrpcClient.GrpcProtocolClientFactory(collectorCertLocation),
              RpcClient.TaskType.GET_SETTINGS,
              RpcClient.TaskType.POST_METRICS,
              RpcClient.TaskType.POST_STATUS);
    } catch (Exception e) {
      logger.warn("Failed to initialize rpc non-tracing client: " + e.getMessage(), e);
    }
  }

  @Override
  public void close() {
    if (tracingRpcClient != null) {
      tracingRpcClient.close();
    }

    if (nonTracingRpcClient != null) {
      nonTracingRpcClient.close();
    }
  }
}
