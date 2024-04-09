package com.solarwinds.opentelemetry.extensions;

import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.joboe.sampling.SettingsArg;

public class FileSettings extends Settings {
  private final JsonSettings jsonSettings;

  public FileSettings(JsonSettings jsonSettings) {
    this.jsonSettings = jsonSettings;
  }

  @Override
  public long getValue() {
    return jsonSettings.getValue();
  }

  @Override
  public long getTimestamp() {
    return jsonSettings.getTimestamp();
  }

  @Override
  public short getType() {
    return jsonSettings.getType();
  }

  @Override
  public short getFlags() {
    short flags = 0;
    String[] flagTokens = jsonSettings.getFlags().split(",");
    for (String flagToken : flagTokens) {
      if ("OVERRIDE".equals(flagToken)) {
        flags |= OBOE_SETTINGS_FLAG_OVERRIDE;
      } else if ("SAMPLE_START".equals(flagToken)) {
        flags |= OBOE_SETTINGS_FLAG_SAMPLE_START;
      } else if ("SAMPLE_THROUGH".equals(flagToken)) {
        flags |= OBOE_SETTINGS_FLAG_SAMPLE_THROUGH;
      } else if ("SAMPLE_THROUGH_ALWAYS".equals(flagToken)) {
        flags |= OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS;
      } else if ("TRIGGER_TRACE".equals(flagToken)) {
        flags |= OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED;
      } else if ("SAMPLE_BUCKET_ENABLED".equals(flagToken)) { // not used anymore
        flags |= OBOE_SETTINGS_FLAG_SAMPLE_BUCKET_ENABLED;
      } else {
        LoggerFactory.getLogger().debug("Unknown flag found from settings: " + flagToken);
      }
    }
    return flags;
  }

  @Override
  public String getLayer() {
    return jsonSettings.getLayer();
  }

  @Override
  public long getTtl() {
    return jsonSettings.getTtl();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getArgValue(SettingsArg<T> settingsArg) {
    Object value = jsonSettings.getArguments().get(settingsArg.getKey());
    if (value != null && settingsArg instanceof SettingsArg.ByteArraySettingsArg) {
      value = value.toString().getBytes();
    }

    return (T) value;
  }
}