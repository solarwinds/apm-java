package com.solarwinds.opentelemetry.extensions.initialize.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.opentelemetry.extensions.config.HttpSettingsFetcher;
import com.solarwinds.opentelemetry.extensions.config.HttpSettingsReader;
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
