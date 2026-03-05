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

import com.solarwinds.joboe.core.Event;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * RPC client that provides various api methods to our collector
 *
 * @author pluk
 */
public interface Client {
  Future<Result> postEvents(List<Event> events, Callback<Result> callback) throws ClientException;

  Future<Result> postMetrics(List<Map<String, Object>> messages, Callback<Result> callback)
      throws ClientException;

  Future<Result> postStatus(List<Map<String, Object>> messages, Callback<Result> callback)
      throws ClientException;

  Future<SettingsResult> getSettings(String version, Callback<SettingsResult> callback)
      throws ClientException;

  void close();

  Status getStatus();

  enum ClientType {
    GRPC
  }

  enum Status {
    NOT_CONNECTED,
    OK,
    FAILURE
  }

  interface Callback<T> {
    void complete(T result);

    void fail(Exception e);
  }
}
