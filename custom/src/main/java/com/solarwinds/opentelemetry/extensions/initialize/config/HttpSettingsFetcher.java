package com.solarwinds.opentelemetry.extensions.initialize.config;

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
