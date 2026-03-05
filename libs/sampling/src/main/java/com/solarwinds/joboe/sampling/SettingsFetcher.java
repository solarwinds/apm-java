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

package com.solarwinds.joboe.sampling;

import java.util.concurrent.CountDownLatch;

/**
 * Fetches {@link Settings}, fetching strategy based on concrete implementation
 *
 * Provides <code>Settings</code> via 2 mechanisms:
 * <ol>
 *  <li>Direct retrieval from <code>getSettings</code></li>
 *  <li>Notify subscribing {@link SettingsListener} from <code>registerListener</code>,
 *  the fetcher implementation determines whenever notification should be sent to subscribing <code>SettingsListener</code>
 *  </li>
 * </ol>
 *
 * Take note that other logics should inquire about <code>Settings<code> via {@link SettingsManager} instead of this fetcher directly
 *
 * @author pluk
 *
 */
public interface SettingsFetcher {
  String DEFAULT_LAYER = "";

  // Settings getSettings(String serviceName);
  Settings getSettings();

  void registerListener(SettingsListener listener);

  CountDownLatch isSettingsAvailableLatch();

  void close();
}
