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

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class EnvConfigReaderTest {
  @Test
  public void testValidRead() throws InvalidConfigException {
    Map<String, String> vars = new HashMap<String, String>();
    vars.put(ConfigProperty.EnvPrefix.PRODUCT + "SERVICE_KEY", "some key");
    EnvConfigReader reader = new EnvConfigReader(vars);
    ConfigContainer container = new ConfigContainer();
    reader.read(container);

    assertEquals("some key", container.get(ConfigProperty.AGENT_SERVICE_KEY));
  }

  /** Even if some values are invalid, it should still read the rest */
  @Test
  public void testPartialRead() {
    Map<String, String> vars = new HashMap<String, String>();
    vars.put(ConfigProperty.EnvPrefix.PRODUCT + "SERVICE_KEY", "some key");
    vars.put(ConfigProperty.EnvPrefix.PRODUCT + "MAX_SQL_QUERY_LENGTH", "2.1");
    EnvConfigReader reader = new EnvConfigReader(vars);
    ConfigContainer container = new ConfigContainer();
    try {
      reader.read(container);
      fail("Expected " + InvalidConfigException.class.getName() + " but it's not thrown");
    } catch (InvalidConfigException e) {
      // expected
    }

    assertEquals("some key", container.get(ConfigProperty.AGENT_SERVICE_KEY));
  }
}
