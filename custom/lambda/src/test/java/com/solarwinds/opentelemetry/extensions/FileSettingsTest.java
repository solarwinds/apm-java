package com.solarwinds.opentelemetry.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileSettingsTest {
  private FileSettings tested;

  @BeforeEach
  void setup() {
    tested =
        new FileSettings(
            JsonSettings.builder()
                .flags(
                    "OVERRIDE,SAMPLE_START,SAMPLE_THROUGH,SAMPLE_THROUGH_ALWAYS,TRIGGER_TRACE,SAMPLE_BUCKET_ENABLED")
                .build());
  }

  @Test
  void return126() {
    assertEquals(126, tested.getFlags());
  }
}
