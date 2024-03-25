package com.solarwinds.opentelemetry.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.solarwinds.joboe.sampling.SamplingException;
import com.solarwinds.joboe.sampling.Settings;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FileSettingsReaderTest {
  private FileSettingsReader tested =
      new FileSettingsReader(
          FileSettingsReader.class.getResource("/solarwinds-apm-settings.json").getPath());

  @Test
  void returnSettings() throws SamplingException {
    Map<String, Settings> settings = tested.getSettings();
    assertEquals(1, settings.size());
  }
}
