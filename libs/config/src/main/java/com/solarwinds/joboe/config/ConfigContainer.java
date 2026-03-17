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

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * This container serves several purposes
 *
 * <p>
 *
 * <ol>
 *   <li>Contains configuration values, the subset method prevents irrelevant access to config
 *       parameters
 *   <li>Type conversion/validation base on string values as parameter in method putByStringValue
 * </ol>
 *
 * <p>Take note that this container does not allow overwriting existing values. If values are
 * inserted multiple times with the same key, only the first insert will be handled, all subsequent
 * inserts are ignored
 *
 * @author Patson Luk
 */
public class ConfigContainer {
  private static final Logger logger = LoggerFactory.getLogger();

  // The map that contains all the config info. First grouped by ConfigGroup (MONITOR, AGENT etc),
  // then by each ConfigProperty. Not directly accessible from the outside
  private final Map<ConfigGroup, Map<ConfigProperty, Object>> configMaps =
      new HashMap<ConfigGroup, Map<ConfigProperty, Object>>();

  /**
   * Subset method should be used when parameters are passed down to other code. It should only pass
   * the set of parameters that are relevant to that particular module, not everything
   *
   * <p>For example, the JMX monitoring code should not have access to the agent parameter such as
   * the sampling rate
   *
   * @param groups groups of ConfigGroup to retain
   * @return
   */
  public ConfigContainer subset(ConfigGroup... groups) {
    ConfigContainer subset = new ConfigContainer();
    for (ConfigGroup group : groups) {
      subset.configMaps.put(group, this.configMaps.get(group));
    }

    return subset;
  }

  /**
   * Gets a configuration property value from a Property key
   *
   * @param propertyKey
   * @return the property value based on the propertyKey
   */
  public Object get(ConfigProperty propertyKey) {
    Map<ConfigProperty, Object> configMap = configMaps.get(propertyKey.getGroup());

    if (configMap != null) {
      return configMap.get(propertyKey);
    } else { // no config for this group configured
      return null;
    }
  }

  /**
   * Checks whether a property key is set in the container
   *
   * @param propertyKey
   * @return whether the property key is set in the container
   */
  public boolean containsProperty(ConfigProperty propertyKey) {
    Map<ConfigProperty, Object> configMap = configMaps.get(propertyKey.getGroup());

    if (configMap != null) {
      return configMap.containsKey(propertyKey);
    } else {
      return false;
    }
  }

  public Object remove(ConfigProperty propertyKey) {
    Map<ConfigProperty, Object> configs = configMaps.get(propertyKey.getGroup());
    if (configs != null) {
      return configs.remove(propertyKey);
    }
    return null;
  }

  public void put(ConfigProperty propertyKey, Object value) throws InvalidConfigException {
    put(propertyKey, value, false);
  }

  public void put(ConfigProperty propertyKey, Object value, boolean override)
      throws InvalidConfigException {
    Map<ConfigProperty, Object> configMap = configMaps.get(propertyKey.getGroup());

    if (configMap == null) { // The Group is not initialized, put a new map into the configMap
      configMap = new HashMap<ConfigProperty, Object>();
      configMaps.put(propertyKey.getGroup(), configMap);
    }

    if (!override
        && configMap.containsKey(
            propertyKey)) { // the key was already inserted before, do NOT overwrite
      if (!configMap.get(propertyKey).equals(value)) {
        logger.debug(
            "key ["
                + propertyKey
                + "] is already defined with value ["
                + configMap.get(propertyKey)
                + "]. Ignoring new value ["
                + value
                + "]");
      }
    } else {
      if (value == null) { // Do not allow null via put by string value
        throw new InvalidConfigException(
            propertyKey, " Does not support null property value", null);
      } else {
        configMap.put(propertyKey, value);
      }
    }
  }

  /**
   * Insert a property value by its String representation. Take note that the String argument will
   * be converted to the type defined in the <code>ConfigProperty.typeClass</code> and converted by
   * the {@link ConfigParser} if one is defined within {@link ConfigProperty}
   *
   * <p>It will ignore the operation if the key already exists
   *
   * @param propertyKey
   * @param stringValue
   * @throws InvalidConfigException if the stringValue cannot be converted to the expected type
   *     defined in the propertyKey
   */
  public void putByStringValue(ConfigProperty propertyKey, String stringValue)
      throws InvalidConfigException {
    Object value = getValue(stringValue, propertyKey);
    if (value == null) { // Do not allow null via put by string value
      throw new InvalidConfigException(
          propertyKey,
          "Does not support null property value, the value ["
              + stringValue
              + "] got converted to null value",
          null);
    } else {
      put(propertyKey, value);
    }
  }

  /**
   * Converts and returns the Object of typeClass base on the valueString. Take note that this
   * method should only handle the basic Wrapper types like Integer, BigDecimal or String. Do not
   * attempt to handle anything too specific here as the Container is supposed to be generic
   *
   * @param valueString
   * @param configProperty
   * @return the Object of typeClass base on the valueString. null if the conversion failed
   * @throws InvalidConfigException
   */
  @SuppressWarnings("unchecked")
  private static <T extends Serializable> Object getValue(
      String valueString, ConfigProperty configProperty) throws InvalidConfigException {
    Serializable javaValue;
    Class<T> typeClass = (Class<T>) configProperty.getTypeClass();
    ConfigParser<T, ?> processor = (ConfigParser<T, ?>) configProperty.getConfigParser();

    try {
      if (typeClass != String.class) { // do not attempt to trim String type
        valueString = valueString.trim();
      }

      if (typeClass == String.class) {
        javaValue = valueString;
      } else if (typeClass == Long.class) {
        javaValue = Long.valueOf(valueString);
      } else if (typeClass == BigDecimal.class) {
        javaValue = new BigDecimal(valueString);
      } else if (typeClass == Integer.class) {
        javaValue = Integer.valueOf(valueString);
      } else if (typeClass == Boolean.class) {
        if ("true".equalsIgnoreCase(valueString)
            || "false"
                .equalsIgnoreCase(
                    valueString)) { // use strict handling, do not allow unexpected/invalid values
          javaValue = Boolean.valueOf(valueString);
        } else {
          throw new InvalidConfigException(
              configProperty, "[" + valueString + "] is not valid boolean value");
        }
      } else if (typeClass == String[].class) {
        javaValue = parseJsonStringArray(valueString);
      } else {
        logger.warn("Unknown type for configuration: " + typeClass.getName());
        javaValue = valueString;
      }
    } catch (IllegalArgumentException e) {
      throw new InvalidConfigException(
          configProperty,
          "Failed to parse value ["
              + valueString
              + "] of class ["
              + typeClass.getName()
              + "], message: "
              + e.getMessage(),
          e);
    }

    if (processor != null) {
      try {
        return processor.convert((T) javaValue);
      } catch (InvalidConfigException e) {
        throw new InvalidConfigException(
            configProperty, e.getMessage(), e.getCause()); // set the violating config property
      } catch (ClassCastException e) {
        throw new InvalidConfigException(
            configProperty,
            "Failed to read config value " + javaValue + ", message : " + e.getMessage(),
            e);
      }
    } else {
      return javaValue;
    }
  }

  private static String[] parseJsonStringArray(String stringArrayValue) {
    List<String> list = new ArrayList<String>();
    try {
      JSONArray stringArray = new JSONArray(stringArrayValue);
      for (int i = 0; i < stringArray.length(); i++) {
        list.add((String) stringArray.get(i));
      }
    } catch (JSONException e) {
      logger.warn(e.getMessage());
      throw new IllegalArgumentException(
          "Cannot parse the string value "
              + stringArrayValue
              + " as json array : "
              + e.getMessage(),
          e);
    }

    return list.toArray(new String[list.size()]);
  }

  @Override
  public String toString() {
    return configMaps.toString();
  }
}
