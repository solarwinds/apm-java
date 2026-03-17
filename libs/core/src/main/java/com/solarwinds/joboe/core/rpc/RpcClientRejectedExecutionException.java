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

import java.util.concurrent.RejectedExecutionException;

/**
 * Indicates collector RPC client rejects the operation before making any actual outbound requests
 *
 * @author pluk
 */
public class RpcClientRejectedExecutionException extends ClientRejectedExecutionException {

  public RpcClientRejectedExecutionException(RejectedExecutionException cause) {
    super(cause);
  }

  public RpcClientRejectedExecutionException(String message) {
    super(message);
  }
}
