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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.sampling.SamplingException;
import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.joboe.sampling.SettingsListener;
import java.util.Collections;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AwsLambdaSettingsFetcherTest {

  @InjectMocks private AwsLambdaSettingsFetcher tested;

  @Mock private FileSettingsReader fileSettingsReaderMock;

  @Mock private Settings settingsMock;

  @Mock private SettingsListener settingsListenerMock;

  @Test
  void returnSettingsWhenNoSettings() throws SamplingException {
    when(fileSettingsReaderMock.getSettings())
        .thenReturn(
            new HashMap<>() {
              {
                put("", settingsMock);
              }
            });

    tested.getSettings();

    verify(fileSettingsReaderMock).getSettings();
  }

  @Test
  void returnSettingsWhenSettingsExpired() throws SamplingException {
    when(fileSettingsReaderMock.getSettings())
        .thenReturn(
            new HashMap<>() {
              {
                put("", settingsMock);
              }
            });

    when(settingsMock.getTimestamp()).thenReturn(0L);
    when(settingsMock.getTtl()).thenReturn(0L);

    tested.getSettings();
    tested.getSettings();

    verify(fileSettingsReaderMock, atMost(2)).getSettings();
  }

  @Test
  void callListenerOnSettingsRetrieved() throws SamplingException {
    when(fileSettingsReaderMock.getSettings())
        .thenReturn(
            new HashMap<>() {
              {
                put("", settingsMock);
              }
            });

    tested.registerListener(settingsListenerMock);
    tested.getSettings();

    verify(settingsListenerMock).onSettingsRetrieved(eq(settingsMock));
  }

  @Test
  void returnNullWhenNoSettings() throws SamplingException {
    when(fileSettingsReaderMock.getSettings()).thenReturn(Collections.emptyMap());

    tested.getSettings();

    verify(fileSettingsReaderMock).getSettings();
  }
}
