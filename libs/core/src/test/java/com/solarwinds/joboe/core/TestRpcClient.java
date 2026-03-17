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
import com.solarwinds.joboe.core.rpc.Result;
import com.solarwinds.joboe.core.rpc.ResultCode;
import com.solarwinds.joboe.core.rpc.SettingsResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import lombok.Getter;

public class TestRpcClient implements Client {
  private final long delay;

  @Getter private final List<Event> postedEvents = new ArrayList<Event>();

  @Getter
  private final List<Map<String, Object>> postedMetrics = new ArrayList<Map<String, Object>>();

  @Getter
  private final List<Map<String, Object>> postedStatus = new ArrayList<Map<String, Object>>();

  private final Result stringResult;
  private final SettingsResult settingsResult;

  public TestRpcClient(long delay) {
    this(delay, ResultCode.OK);
  }

  public TestRpcClient(long delay, ResultCode resultCode) {
    super();
    this.delay = delay;
    stringResult = new Result(resultCode, "", "");
    settingsResult = new SettingsResult(resultCode, "", "", Collections.emptyList());
  }

  @Override
  public Future<Result> postEvents(List<Event> events, Callback<Result> callback) {
    sleep();
    postedEvents.addAll(events);
    return CompletableFuture.completedFuture(stringResult);
  }

  @Override
  public Future<Result> postMetrics(List<Map<String, Object>> messages, Callback<Result> callback) {
    sleep();
    postedMetrics.addAll(messages);
    return CompletableFuture.completedFuture(stringResult);
  }

  @Override
  public Future<Result> postStatus(List<Map<String, Object>> messages, Callback<Result> callback) {
    sleep();
    postedStatus.addAll(messages);
    return CompletableFuture.completedFuture(stringResult);
  }

  @Override
  public Future<SettingsResult> getSettings(String version, Callback<SettingsResult> callback) {
    sleep();
    return CompletableFuture.completedFuture(settingsResult);
  }

  @Override
  public void close() {}

  @Override
  public Status getStatus() {
    return Status.OK;
  }

  private void sleep() {
    if (delay > 0) {
      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public void reset() {
    postedEvents.clear();
    postedMetrics.clear();
    postedStatus.clear();
  }
}
