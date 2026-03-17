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

package com.solarwinds.joboe.core.rpc;

import com.solarwinds.joboe.core.rpc.grpc.GrpcClientManager;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ClientManagerProvider {
  private ClientManagerProvider() {}

  private static final Logger logger = LoggerFactory.getLogger();
  private static final Map<Client.ClientType, RpcClientManager> registeredManagers =
      new HashMap<>();

  static {
    registeredManagers.put(Client.ClientType.GRPC, new GrpcClientManager());
  }

  public static Optional<RpcClientManager> getClientManager(Client.ClientType clientType) {
    logger.debug("Using " + clientType + " for rpc calls");
    return Optional.ofNullable(registeredManagers.get(clientType));
  }

  public static void closeAllManagers() {
    for (RpcClientManager clientManager : registeredManagers.values()) {
      clientManager.close();
    }
  }
}
