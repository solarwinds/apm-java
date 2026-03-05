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

package com.solarwinds.joboe.core.settings;

import com.solarwinds.joboe.core.util.DaemonThreadFactory;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.joboe.sampling.SettingsFetcher;
import com.solarwinds.joboe.sampling.SettingsListener;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A {@link SettingsFetcher} that polls settings from the provided {@link SettingsReader} at a given
 * refresh interval
 *
 * <p>Every time a {@link Settings} is fetched, the registered listener would be notified
 *
 * @author pluk
 */
public class PollingSettingsFetcher implements SettingsFetcher {
  private static final Logger logger = LoggerFactory.getLogger();
  private static final int DEAFULT_REFRESH_INTERVAL = 30; // 30 secs

  private final int refreshInterval; // pauses between each reader call in seconds
  private final SettingsReader reader;

  private final ExecutorService executorService =
      Executors.newSingleThreadExecutor(DaemonThreadFactory.newInstance("poll-settings"));
  private Settings currentSettings;

  private SettingsListener listener;
  private final CountDownLatch firstTryLatch = new CountDownLatch(1);
  private boolean active = true;

  public PollingSettingsFetcher(SettingsReader reader) {
    this(reader, DEAFULT_REFRESH_INTERVAL);
  }

  public PollingSettingsFetcher(SettingsReader reader, int refreshInterval) {
    this.reader = reader;
    this.refreshInterval = refreshInterval;
    startWorker();
  }

  @Override
  public Settings getSettings() {
    synchronized (this) {
      if (isExpired(currentSettings)) {
        currentSettings = null;
      }
    }
    return currentSettings;
  }

  private boolean isExpired(Settings settings) {
    boolean isExpired = false;
    if (settings != null) {
      long ttlInMillisec = settings.getTtl() * 1000;
      isExpired = settings.getTimestamp() + ttlInMillisec < System.currentTimeMillis();
      if (isExpired) {
        logger.warn("Settings for [" + settings + "] has expired");
      }
    }
    return isExpired;
  }

  @Override
  public CountDownLatch isSettingsAvailableLatch() {
    return firstTryLatch;
  }

  @Override
  public void registerListener(SettingsListener listener) {
    this.listener = listener;
  }

  @Override
  public void close() {
    active = false;
    executorService.shutdown();
    if (reader != null) {
      reader.close();
    }
  }

  /**
   * Starts background worker to update settings record with <code>refreshInterval</code> sec pauses
   * in between
   *
   * @return
   */
  private Future<?> startWorker() {
    return executorService.submit(
        new Runnable() {
          @Override
          public void run() {
            logger.debug("Starting background worker to update settings");
            boolean running = true;
            while (PollingSettingsFetcher.this.active
                && running) { // periodically poll for settings
              Settings newSettings = null;
              try {
                newSettings = reader.getSettings();
              } catch (OboeSettingsException e) {
                logger.debug(
                    "Failed to get settings : " + e.getMessage(),
                    e); // Should not be too noisy as this might happen for intermittent connection
                // problem
              }

              if (newSettings != null) {
                synchronized (this) {
                  currentSettings = newSettings;
                }

                firstTryLatch.countDown(); // count down on the latch to flag that settings is ready
                if (listener != null) {
                  listener.onSettingsRetrieved(newSettings);
                }
              } else { // purge expired ones
                logger.debug("Failed to retrieve settings from rpc call");
              }

              try {
                TimeUnit.SECONDS.sleep(refreshInterval); // sleep for the defined interval
              } catch (InterruptedException e) {
                logger.debug(
                    PollingSettingsFetcher.class.getName()
                        + " worker is interrupted : "
                        + e.getMessage());
                running = false;
              }
            }
          }
        });
  }
}
