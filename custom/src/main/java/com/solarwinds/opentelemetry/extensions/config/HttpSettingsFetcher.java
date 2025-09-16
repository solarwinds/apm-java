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

import com.solarwinds.joboe.core.util.DaemonThreadFactory;
import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.joboe.sampling.SettingsFetcher;
import com.solarwinds.joboe.sampling.SettingsListener;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HttpSettingsFetcher implements SettingsFetcher {
  private final HttpSettingsReader httpSettingsReader;

  private SettingsListener settingsListener;

  private Settings currentSettings;

  private final long interval;

  private final CountDownLatch latch = new CountDownLatch(1);

  ScheduledExecutorService scheduledExecutorService =
      Executors.newSingleThreadScheduledExecutor(DaemonThreadFactory.newInstance("poll-settings"));

  public HttpSettingsFetcher(HttpSettingsReader httpSettingsReader, long interval) {
    this.interval = interval;
    this.httpSettingsReader = httpSettingsReader;
    pollSettings();
  }

  @Override
  public Settings getSettings() {
    if (currentSettings == null
        || currentSettings.getTimestamp() + currentSettings.getTtl()
            < System.currentTimeMillis() / 1000) {
      currentSettings = null;
    }

    return currentSettings;
  }

  private void pollSettings() {
    scheduledExecutorService.scheduleAtFixedRate(
        () -> {
          currentSettings = httpSettingsReader.getSettings();
          if (settingsListener != null) {
            settingsListener.onSettingsRetrieved(currentSettings);
          }
          latch.countDown();
        },
        0,
        interval,
        TimeUnit.SECONDS);
  }

  @Override
  public void registerListener(SettingsListener settingsListener) {
    this.settingsListener = settingsListener;
  }

  @Override
  public CountDownLatch isSettingsAvailableLatch() {
    return latch;
  }

  @Override
  public void close() {
    scheduledExecutorService.shutdown();
  }
}
