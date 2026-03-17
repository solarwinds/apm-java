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

package com.solarwinds.joboe.core;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Converter that converts any object into object that are safe to used as the value of Event Info
 *
 * @author Patson Luk
 */
public class EventValueConverter {
  protected static final Logger logger = LoggerFactory.getLogger();

  // types that can put directly to the Event info AND the type has to be final (no isInstanceOf
  // checks are going to be performed)
  private static final Set<Class<?>> EXPECTED_SIMPLE_TYPES =
      new HashSet<Class<?>>(
          Arrays.asList(
              new Class<?>[] {
                Boolean.class, Double.class, Integer.class, Long.class,
              }));

  // types that requires conversion/validations before putting into the Event info, more common
  // class first for better performance (since we might do instanceOf check)
  protected final Map<Class<?>, Converter<?, ?>> EXPECTED_SPECIAL_TYPES =
      new LinkedHashMap<Class<?>, Converter<?, ?>>();

  // By default, always output the class name only if its an unknown parameter type
  private static final Converter<?, ?> DEFAULT_HANDLER = new ClassNameParameterHandler();

  public static final int DEFAULT_MAX_LENGTH = 1024;

  // Max character counts allowed by any parameter that has string representation as tracked value
  public final int maxValueLength;
  private final Logger.Level logLevel;

  public EventValueConverter() {
    this(DEFAULT_MAX_LENGTH);
  }

  public EventValueConverter(int maxValueLength) {
    this(maxValueLength, Logger.Level.INFO);
  }

  public EventValueConverter(int maxValueLength, Logger.Level logLevel) {
    EXPECTED_SPECIAL_TYPES.put(String.class, new SimpleParameterHandler());
    EXPECTED_SPECIAL_TYPES.put(Date.class, new SimpleParameterHandler());
    EXPECTED_SPECIAL_TYPES.put(byte[].class, new ByteArrayParameterHandler());
    EXPECTED_SPECIAL_TYPES.put(URL.class, new ToStringParameterHandler());
    EXPECTED_SPECIAL_TYPES.put(
        BigDecimal.class, new BigDecimalParameterHandler()); // Event does not support BigDecimal
    EXPECTED_SPECIAL_TYPES.put(
        BigInteger.class, new BigIntegerParameterHandler()); // Event does not support BigInteger
    EXPECTED_SPECIAL_TYPES.put(
        Float.class, new FloatParameterHandler()); // Event does not support Float
    EXPECTED_SPECIAL_TYPES.put(
        Short.class, new ShortParameterHandler()); // Event does not support Short
    EXPECTED_SPECIAL_TYPES.put(
        Byte.class, new ByteParameterHandler()); // Event does not support Byte
    EXPECTED_SPECIAL_TYPES.put(Character.class, new ToStringParameterHandler());
    EXPECTED_SPECIAL_TYPES.put(InetAddress.class, new ToStringParameterHandler());
    EXPECTED_SPECIAL_TYPES.put(Collection.class, new CollectionParameterHandler());
    EXPECTED_SPECIAL_TYPES.put(Map.class, new MapParameterHandler());
    EXPECTED_SPECIAL_TYPES.put(UUID.class, new ToStringParameterHandler());

    this.maxValueLength = maxValueLength;
    this.logLevel = logLevel;
  }

  /**
   * Converts the rawValue into an object that is acceptable as info value of Event
   *
   * @param rawValue
   * @return
   */
  public Object convertToEventValue(Object rawValue) {
    if (rawValue == null) {
      return null;
    } else if (EXPECTED_SIMPLE_TYPES.contains(
        rawValue
            .getClass())) { // quick check to avoid overhead of checking for each of the entry, most
      // objects should fall into this condition
      return rawValue; // safe to put directly to the map
    } else {
      return getValueFromSpecialType(rawValue);
    }
  }

  /**
   * Gets the result value that we will use for tracking, some parameter values might need
   * conversions
   *
   * @param parameter the input parameter, the one used by the setXXX and setObject methods
   * @return the result value that we will stored and used when reporting the parameters in Trace
   */
  @SuppressWarnings("unchecked")
  private Object getValueFromSpecialType(Object parameter) {
    Converter converter = null;

    // quick check to avoid overhead of checking for each of the entries
    if (EXPECTED_SPECIAL_TYPES.containsKey(parameter.getClass())) {
      converter = EXPECTED_SPECIAL_TYPES.get(parameter.getClass());
    } else { // have to iterate thru the special types, should not be common
      for (Class<?> specialType : EXPECTED_SPECIAL_TYPES.keySet()) {
        if (specialType.isInstance(parameter)) {
          converter = EXPECTED_SPECIAL_TYPES.get(specialType);
          break;
        }
      }
    }

    if (converter == null) {
      logger.debug(
          "Class ["
              + parameter.getClass().getName()
              + "] is not in the expected list of classes for PreparedStatement parameter. Using the default handler");
      converter = DEFAULT_HANDLER;
    }

    // use the handler to retrieve the result value
    Object value = converter.getValue(parameter);

    // trim the result value if it is a String
    if (value instanceof String) {
      String stringValue = (String) value;
      if (stringValue.length() > maxValueLength) {
        logger.log(
            logLevel,
            "Parameter truncated as it is too long [" + stringValue.length() + "] characters");
        int truncateCount = stringValue.length() - maxValueLength;
        stringValue =
            stringValue.substring(0, maxValueLength)
                + "...("
                + truncateCount
                + " characters truncated)";
      }
      return stringValue;
    } else {
      return value;
    }
  }

  /**
   * Handler that takes in an input parameter value and convert it in something we can use for
   * tracking (in BSON map)
   *
   * @author Patson Luk
   * @param <T> Type of the Parameter
   * @param <R> Expected Type of the result value
   */
  protected abstract static class Converter<T, R> {
    protected abstract R getValue(T parameter);
  }

  /**
   * Gets the String value of the object by using the toString() method of the input parameter
   *
   * @author Patson Luk
   */
  public static class ToStringParameterHandler extends Converter<Object, String> {
    @Override
    protected String getValue(Object parameter) {
      return parameter.toString();
    }
  }

  /**
   * Gets the fully qualified class name of the input parameter
   *
   * @author Patson Luk
   */
  public static class ClassNameParameterHandler extends Converter<Object, String> {
    @Override
    protected String getValue(Object parameter) {
      return "("
          + parameter.getClass().getName()
          + ") id ["
          + System.identityHashCode(parameter)
          + "]";
    }
  }

  /**
   * Byte array handler. Display the length of the Byte array
   *
   * @author Patson Luk
   */
  private static class ByteArrayParameterHandler extends Converter<byte[], String> {
    @Override
    protected String getValue(byte[] parameter) {
      return "(Byte array "
          + parameter.length
          + " Bytes) id ["
          + System.identityHashCode(parameter)
          + "]";
    }
  }

  private static class CollectionParameterHandler extends Converter<Collection<?>, String> {
    @Override
    protected String getValue(Collection<?> parameter) {
      return "(Collection of class ["
          + parameter.getClass().getName()
          + "] with "
          + parameter.size()
          + " Elements) id ["
          + System.identityHashCode(parameter)
          + "]";
    }
  }

  private static class MapParameterHandler extends Converter<Map<?, ?>, String> {
    @Override
    protected String getValue(Map<?, ?> parameter) {
      return "(Map of class ["
          + parameter.getClass().getName()
          + "] with "
          + parameter.size()
          + " Elements) id ["
          + System.identityHashCode(parameter)
          + "]";
    }
  }

  /**
   * BigDecimal handler. Convert to double instead
   *
   * @author Patson Luk
   */
  private static class BigDecimalParameterHandler extends Converter<BigDecimal, Double> {
    @Override
    protected Double getValue(BigDecimal parameter) {
      return parameter.doubleValue();
    }
  }

  /**
   * BigInteger handler. Convert to long instead
   *
   * @author Patson Luk
   */
  private static class BigIntegerParameterHandler extends Converter<BigInteger, Long> {
    @Override
    protected Long getValue(BigInteger parameter) {
      return parameter.longValue();
    }
  }

  /**
   * Float handler. Convert to double instead
   *
   * @author Patson Luk
   */
  private static class FloatParameterHandler extends Converter<Float, Double> {
    @Override
    protected Double getValue(Float parameter) {
      return parameter.doubleValue();
    }
  }

  /**
   * Short handler. Convert to integer instead
   *
   * @author Patson Luk
   */
  private static class ShortParameterHandler extends Converter<Short, Integer> {
    @Override
    protected Integer getValue(Short parameter) {
      return parameter.intValue();
    }
  }

  /**
   * Byte handler. Convert to integer instead
   *
   * @author Patson Luk
   */
  private static class ByteParameterHandler extends Converter<Byte, Integer> {
    @Override
    protected Integer getValue(Byte parameter) {
      return (int) parameter;
    }
  }

  /**
   * Simple Parameter handler. No special handling is done, the parameter will simply be returned
   * "as is"
   *
   * @author Patson Luk
   */
  private static class SimpleParameterHandler extends Converter<Object, Object> {
    @Override
    protected Object getValue(Object parameter) {
      return parameter;
    }
  }
}
