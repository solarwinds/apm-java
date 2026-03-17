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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * A Reader that reads the input config file in JSON
 *
 * @author Patson Luk
 */
public class JsonConfigReader extends ConfigReader {
  private final InputStream configStream;

  /**
   * @param configStream The input stream of the configuration file
   */
  public JsonConfigReader(InputStream configStream) {
    super(ConfigSourceType.JSON_FILE);
    this.configStream = configStream;
  }

  @Override
  public void read(ConfigContainer container) throws InvalidConfigException {
    if (configStream == null) {
      throw new InvalidConfigException("Cannot find any valid configuration for agent");
    }

    List<InvalidConfigException> exceptions = new ArrayList<InvalidConfigException>();
    JSONObject jsonObject;
    try {
      jsonObject = new JSONObject(new JSONTokener(configStream));

      for (Object keyAsObject : jsonObject.keySet()) {
        ConfigProperty key =
            ConfigProperty.fromConfigFileKey(
                (String)
                    keyAsObject); // attempt to retrieve the corresponding ConfigProperty as key

        if (key == null) {
          exceptions.add(
              new InvalidConfigException(
                  "Invalid line in configuration file : key [" + keyAsObject + "] is invalid"));
        } else {
          try {
            Object value = jsonObject.get((String) keyAsObject);
            if (value != null) {
              container.putByStringValue(key, value.toString());
            } else {
              // should not be null since it is read from JSON file
              exceptions.add(new InvalidConfigException(key, "Unexpected null value", null));
            }
          } catch (JSONException e) {
            exceptions.add(
                new InvalidConfigException(
                    "Json exception while processing config for key ["
                        + keyAsObject
                        + "] : "
                        + e.getMessage(),
                    e));
          }
        }
      }
    } catch (JSONException e) {
      exceptions.add(
          new InvalidConfigException(
              "Json exception while processing config : " + e.getMessage(), e));
    }

    if (!exceptions.isEmpty()) {
      logger.warn(
          "Found " + exceptions.size() + " exception(s) while reading config from config file");
      throw exceptions.get(0); // report the first exception encountered
    }
  }
}
