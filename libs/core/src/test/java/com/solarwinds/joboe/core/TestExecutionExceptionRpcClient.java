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
import com.solarwinds.joboe.core.rpc.Result;
import com.solarwinds.joboe.core.rpc.SettingsResult;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TestExecutionExceptionRpcClient implements Client {
  private final ExecutorService service = Executors.newSingleThreadExecutor();

  @Override
  public Future<Result> postEvents(List<Event> events, Callback<Result> callback) {
    return service.submit(new ExceptionCallable<Result>());
  }

  @Override
  public Future<Result> postMetrics(List<Map<String, Object>> messages, Callback<Result> callback) {
    return service.submit(new ExceptionCallable<Result>());
  }

  @Override
  public Future<Result> postStatus(List<Map<String, Object>> messages, Callback<Result> callback) {
    return service.submit(new ExceptionCallable<Result>());
  }

  @Override
  public Future<SettingsResult> getSettings(String version, Callback<SettingsResult> callback) {
    return service.submit(new ExceptionCallable<SettingsResult>());
  }

  private static class ExceptionCallable<T> implements Callable<T> {
    @Override
    public T call() throws Exception {
      throw new ClientException("testing exception from exception rpc client");
    }
  }

  @Override
  public Status getStatus() {
    return Status.OK;
  }

  @Override
  public void close() {
    service.shutdown();
  }
}
