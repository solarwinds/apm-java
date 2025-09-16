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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.sampling.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HttpSettingsFetcherTest {

  @Mock private HttpSettingsReader httpSettingsReaderMock;

  @Mock private Settings settingsMock;

  @Test
  void verifyThatSettingsIsRetrieved() throws InterruptedException {
    when(httpSettingsReaderMock.getSettings()).thenReturn(settingsMock);
    when(settingsMock.getTimestamp()).thenReturn(System.currentTimeMillis());
    when(settingsMock.getTtl()).thenReturn(100000L);

    HttpSettingsFetcher httpSettingsFetcher = new HttpSettingsFetcher(httpSettingsReaderMock, 1);
    httpSettingsFetcher.isSettingsAvailableLatch().await();
    Settings settings = httpSettingsFetcher.getSettings();

    assertEquals(settingsMock, settings);
  }
}
