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

package com.solarwinds.joboe.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class JsonConfigReaderTest {
  @Test
  public void testValidRead() throws InvalidConfigException {
    JsonConfigReader reader =
        new JsonConfigReader(JsonConfigReaderTest.class.getResourceAsStream("/valid.json"));
    ConfigContainer container = new ConfigContainer();
    reader.read(container);

    assertEquals("info", container.get(ConfigProperty.AGENT_LOGGING));
    assertEquals("some key", container.get(ConfigProperty.AGENT_SERVICE_KEY));
  }

  /** Even if some values are invalid, it should still read the rest */
  @Test
  public void testPartialRead() {
    JsonConfigReader reader =
        new JsonConfigReader(JsonConfigReaderTest.class.getResourceAsStream("/invalid.json"));
    ConfigContainer container = new ConfigContainer();
    try {
      reader.read(container);
      fail("Expected " + InvalidConfigException.class.getName() + " but it's not thrown");
    } catch (InvalidConfigException e) {
      // expected
    }

    // the rest of the values should be read
    assertEquals("info", container.get(ConfigProperty.AGENT_LOGGING));
    assertEquals("some key", container.get(ConfigProperty.AGENT_SERVICE_KEY));
  }
}
