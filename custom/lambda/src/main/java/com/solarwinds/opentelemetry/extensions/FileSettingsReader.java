package com.solarwinds.opentelemetry.extensions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.SamplingException;
import com.solarwinds.joboe.sampling.Settings;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileSettingsReader {
  private final String settingsFilePath;

  private static final Logger logger = LoggerFactory.getLogger();

  private static final Gson gson = new GsonBuilder().create();
  private final Type type = new TypeToken<List<JsonSettings>>() {}.getType();

  public FileSettingsReader(String settingsFilePath) {
    this.settingsFilePath = settingsFilePath;
  }

  public Map<String, Settings> getSettings() throws SamplingException {
    try {
      byte[] bytes = Files.readAllBytes(Paths.get(settingsFilePath));
      Map<String, Settings> kvSetting =
          convertToSettingsMap(gson.fromJson(new String(bytes), type));
      logger.debug(String.format("Got settings from file: %s", kvSetting));

      return kvSetting;

    } catch (IOException e) {
      logger.debug(String.format("Failed to read settings from file, error: %s", e));
      throw new SamplingException("Error reading settings from file");
    }
  }

  private Map<String, Settings> convertToSettingsMap(List<JsonSettings> jsonSettings) {
    return jsonSettings.stream()
        .collect(Collectors.toMap(JsonSettings::getLayer, FileSettings::new));
  }
}
