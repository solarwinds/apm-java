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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.solarwinds.joboe.sampling.SamplingException;
import com.solarwinds.joboe.sampling.SettingsArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileSettingsTest {
  private FileSettings tested;

  private final FileSettingsReader reader =
      new FileSettingsReader(
          FileSettingsTest.class.getResource("/solarwinds-apm-settings.json").getPath());

  @BeforeEach
  void setup() throws SamplingException {
    tested = (FileSettings) reader.getSettings();
  }

  @Test
  void return126() {
    assertEquals(116, tested.getFlags());
  }

  @Test
  void testReadArgs() {
    assertDoesNotThrow(() -> tested.getArgValue(SettingsArg.BUCKET_CAPACITY));
    assertDoesNotThrow(() -> tested.getArgValue(SettingsArg.METRIC_FLUSH_INTERVAL));
  }
}
