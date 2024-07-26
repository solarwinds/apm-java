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

package com.solarwinds.opentelemetry.extensions;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.SamplingException;
import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.joboe.sampling.SettingsFetcher;
import com.solarwinds.joboe.sampling.SettingsListener;
import java.util.concurrent.CountDownLatch;

public class AwsLambdaSettingsFetcher implements SettingsFetcher {
  private static final Logger logger = LoggerFactory.getLogger();

  private final FileSettingsReader settingsReader;

  private SettingsListener settingsListener;

  private Settings currentSettings;

  public AwsLambdaSettingsFetcher(FileSettingsReader settingsReader) {
    this.settingsReader = settingsReader;
  }

  @Override
  public Settings getSettings() {
    Settings settings = currentSettings;
    if (settings == null
        || System.currentTimeMillis() - settings.getTimestamp() > settings.getTtl() * 1000) {
      try {
        settings = settingsReader.getSettings();

      } catch (SamplingException e) {
        logger.debug(
            "Failed to get settings : " + e.getMessage(),
            e); // Should not be too noisy as this might happen for intermittent connection problem
      }

      if (settingsListener != null) {
        settingsListener.onSettingsRetrieved(settings);
      }
    }

    currentSettings = settings;
    return settings;
  }

  @Override
  public void registerListener(SettingsListener listener) {
    settingsListener = listener;
  }

  @Override
  public CountDownLatch isSettingsAvailableLatch() {
    return new CountDownLatch(0);
  }

  @Override
  public void close() {}
}
