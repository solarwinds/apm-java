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

import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.joboe.sampling.SettingsFetcher;
import com.solarwinds.joboe.sampling.SettingsListener;
import java.util.concurrent.CountDownLatch;

/**
 * A testing fetcher that returns {@link Settings} by directly reads from the {@link
 * TestSettingsReader} provided in constructor
 *
 * <p>Notifies {@link SettingsListener} whenever the reader has settings changes
 *
 * @author pluk
 */
public class SimpleSettingsFetcher implements SettingsFetcher {
  private final TestSettingsReader reader;
  private SettingsListener listener;

  public SimpleSettingsFetcher(TestSettingsReader reader) {
    this.reader = reader;

    reader.onSettingsChanged(() -> fetch());
  }

  @Override
  public Settings getSettings() {
    try {
      return reader.getSettings();
    } catch (OboeSettingsException e) {
      return null;
    }
  }

  @Override
  public void registerListener(SettingsListener listener) {
    this.listener = listener;
  }

  private void fetch() {
    if (listener != null) {
      Settings newSettings = getSettings();
      listener.onSettingsRetrieved(newSettings);
    }
  }

  @Override
  public CountDownLatch isSettingsAvailableLatch() {
    return new CountDownLatch(0);
  }

  @Override
  public void close() {
    if (reader != null) {
      reader.close();
    }
  }

  public SettingsReader getReader() {
    return reader;
  }
}
