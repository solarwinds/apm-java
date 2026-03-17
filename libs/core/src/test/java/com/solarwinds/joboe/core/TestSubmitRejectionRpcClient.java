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

import com.solarwinds.joboe.core.rpc.Client;
import com.solarwinds.joboe.core.rpc.ClientException;
import com.solarwinds.joboe.core.rpc.ClientRejectedExecutionException;
import com.solarwinds.joboe.core.rpc.Result;
import com.solarwinds.joboe.core.rpc.SettingsResult;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

public class TestSubmitRejectionRpcClient implements Client {
  @Override
  public Future<Result> postEvents(List<Event> events, Callback<Result> callback)
      throws ClientException {
    throw new ClientRejectedExecutionException(
        new RejectedExecutionException("Testing submit client exception"));
  }

  @Override
  public Future<Result> postMetrics(List<Map<String, Object>> messages, Callback<Result> callback)
      throws ClientException {
    throw new ClientRejectedExecutionException(
        new RejectedExecutionException("Testing submit client exception"));
  }

  @Override
  public Future<Result> postStatus(List<Map<String, Object>> messages, Callback<Result> callback)
      throws ClientException {
    throw new ClientRejectedExecutionException(
        new RejectedExecutionException("Testing submit client exception"));
  }

  @Override
  public Future<SettingsResult> getSettings(String version, Callback<SettingsResult> callback)
      throws ClientException {
    throw new ClientRejectedExecutionException(
        new RejectedExecutionException("Testing submit client exception"));
  }

  @Override
  public void close() {}

  @Override
  public Status getStatus() {
    return Status.OK;
  }
}
