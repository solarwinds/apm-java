package com.solarwinds.opentelemetry.extensions;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.SamplingException;
import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.joboe.sampling.SettingsFetcher;
import com.solarwinds.joboe.sampling.SettingsListener;
import java.util.Map;
import java.util.Optional;
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
        Map<String, Settings> allSettings = settingsReader.getSettings();
        if (allSettings.isEmpty()) {
          logger.warn("No Settings returned by the Settings Reader!");

        } else {
          settings = allSettings.get(DEFAULT_LAYER);
          if (settings == null) {
            logger.debug("Cannot find Settings with empty ID. Trying to locate the default record");
            Optional<Settings> defaultSetting =
                allSettings.values().stream().filter(Settings::isDefault).findFirst();

            if (defaultSetting.isPresent()) {
              settings = defaultSetting.get();
            } else {
              logger.warn(
                  "Cannot find Settings with empty ID nor a default record... using the first record");
              settings = allSettings.values().iterator().next();
            }
          }
        }

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
