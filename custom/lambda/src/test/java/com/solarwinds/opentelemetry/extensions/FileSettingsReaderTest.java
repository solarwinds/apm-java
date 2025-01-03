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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.solarwinds.joboe.sampling.SamplingException;
import com.solarwinds.joboe.sampling.Settings;
import org.junit.jupiter.api.Test;

class FileSettingsReaderTest {
  private final FileSettingsReader tested =
      new FileSettingsReader(
          FileSettingsReader.class.getResource("/solarwinds-apm-settings.json").getPath());

  @Test
  void returnSettings() throws SamplingException {
    Settings settings = tested.getSettings();
    assertNotNull(settings);
  }
}
