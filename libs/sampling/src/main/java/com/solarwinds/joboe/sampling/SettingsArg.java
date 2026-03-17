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

package com.solarwinds.joboe.sampling;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * Setting arguments used in {@link Settings}
 *
 * @author pluk
 * @param <T> the corresponding type of the argument value
 */
public abstract class SettingsArg<T> {
  private static final Map<String, SettingsArg<?>> keyToArgs =
      new HashMap<String, SettingsArg<?>>();
  protected Logger logger = LoggerFactory.getLogger();

  public static final SettingsArg<Double> BUCKET_CAPACITY = new DoubleSettingsArg("BucketCapacity");
  public static final SettingsArg<Double> BUCKET_RATE = new DoubleSettingsArg("BucketRate");
  public static final SettingsArg<Integer> METRIC_FLUSH_INTERVAL =
      new IntegerSettingsArg("MetricsFlushInterval");
  public static final SettingsArg<Integer> MAX_TRANSACTIONS =
      new IntegerSettingsArg("MaxTransactions");
  public static final SettingsArg<Integer> MAX_CONTEXT_AGE =
      new IntegerSettingsArg("MaxContextAge");
  public static final SettingsArg<Integer> MAX_CONTEXT_EVENTS =
      new IntegerSettingsArg("MaxContextEvents");
  public static final SettingsArg<Integer> MAX_CONTEXT_BACKTRACES =
      new IntegerSettingsArg("MaxContextBacktraces");
  public static final SettingsArg<Boolean> DISABLE_INHERIT_CONTEXT =
      new BooleanSettingsArg("DisableInheritContext");
  public static final SettingsArg<Integer> MAX_CUSTOM_METRICS =
      new IntegerSettingsArg("MaxCustomMetrics");
  public static final SettingsArg<Integer> EVENTS_FLUSH_INTERVAL =
      new IntegerSettingsArg("EventsFlushInterval");
  public static final SettingsArg<Integer> PROFILING_INTERVAL =
      new IntegerSettingsArg("ProfilingInterval");
  public static final SettingsArg<byte[]> TRACE_OPTIONS_SECRET =
      new ByteArraySettingsArg("SignatureKey");
  public static final SettingsArg<Double> RELAXED_BUCKET_CAPACITY =
      new DoubleSettingsArg("TriggerRelaxedBucketCapacity");
  public static final SettingsArg<Double> RELAXED_BUCKET_RATE =
      new DoubleSettingsArg("TriggerRelaxedBucketRate");
  public static final SettingsArg<Double> STRICT_BUCKET_CAPACITY =
      new DoubleSettingsArg("TriggerStrictBucketCapacity");
  public static final SettingsArg<Double> STRICT_BUCKET_RATE =
      new DoubleSettingsArg("TriggerStrictBucketRate");

  /**
   * -- GETTER -- Gets the string key of this <code>SettingsArg</code>
   *
   * @return
   */
  @Getter protected final String key;

  private SettingsArg(String key) {
    this.key = key;
    keyToArgs.put(key, this);
  }

  /**
   * Reads byteBuffer and returns the converted argument value
   *
   * @param byteBuffer
   * @return
   */
  public abstract T readValue(ByteBuffer byteBuffer);

  /** Converts the given {@link Object} to T */
  public abstract T readValue(Object object);

  /**
   * Reads the typed value of this SettingsArg and convert it to ByteBuffer
   *
   * @param fromValue
   * @return
   */
  public abstract ByteBuffer toByteBuffer(T fromValue);

  /**
   * Checks if the values of this settings arg type are considered equal. By default uses the equals
   * method.
   *
   * @param value1
   * @param value2
   * @return
   */
  public boolean areValuesEqual(T value1, T value2) {
    if (value1 == value2) {
      return true;
    } else if (value1 == null) {
      return false;
    } else {
      return value1.equals(value2);
    }
  }

  /**
   * Gets the <code>SettingsArg</code> corresponds to this key
   *
   * @param key
   * @return
   */
  public static SettingsArg<?> fromKey(String key) {
    return keyToArgs.get(key);
  }

  /**
   * Gets all the available (instantiated so far) <code>SettingsArg</code> instances
   *
   * @return
   */
  public static Collection<SettingsArg<?>> values() {
    return keyToArgs.values();
  }

  @Override
  public String toString() {
    return "SettingsArg [key=" + key + "]";
  }

  public static class DoubleSettingsArg extends SettingsArg<Double> {
    DoubleSettingsArg(String key) {
      super(key);
    }

    @Override
    public Double readValue(ByteBuffer byteBuffer) {
      try {
        return byteBuffer.order(ByteOrder.LITTLE_ENDIAN).getDouble();
      } catch (BufferUnderflowException e) {
        logger.warn(
            "Cannot find valid value for arg ["
                + key
                + "] from settings : "
                + e.getClass().getName());
        return null;
      } finally {
        byteBuffer.rewind(); // cast for JDK 8- runtime compatibility
      }
    }

    @Override
    public Double readValue(Object object) {
      if (object instanceof Integer) {
        return ((Integer) object).doubleValue();

      } else if (object instanceof Double) {
        return (Double) object;
      }

      return null;
    }

    @Override
    public ByteBuffer toByteBuffer(Double value) {
      if (value != null) {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(value);

        buffer.rewind();

        return buffer;
      } else {
        return null;
      }
    }
  }

  public static class IntegerSettingsArg extends SettingsArg<Integer> {
    IntegerSettingsArg(String key) {
      super(key);
    }

    @Override
    public Integer readValue(ByteBuffer byteBuffer) {
      try {
        return byteBuffer.order(ByteOrder.LITTLE_ENDIAN).getInt();
      } catch (BufferUnderflowException e) {
        logger.warn(
            "Cannot find valid value for arg ["
                + key
                + "] from settings : "
                + e.getClass().getName());
        return null;
      } finally {
        byteBuffer.rewind(); // cast for JDK 8- runtime compatibility
      }
    }

    @Override
    public Integer readValue(Object object) {
      if (object instanceof Double) {
        return ((Double) object).intValue();

      } else if (object instanceof Integer) {
        return (Integer) object;
      }

      return null;
    }

    @Override
    public ByteBuffer toByteBuffer(Integer value) {
      if (value != null) {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(value);

        buffer.rewind();

        return buffer;
      } else {
        return null;
      }
    }
  }

  public static class BooleanSettingsArg extends SettingsArg<Boolean> {
    BooleanSettingsArg(String key) {
      super(key);
    }

    @Override
    public Boolean readValue(ByteBuffer byteBuffer) {
      try {
        int value = byteBuffer.order(ByteOrder.LITTLE_ENDIAN).getInt();
        return value != 0; // any non-zero is considered as True
      } catch (BufferUnderflowException e) {
        logger.warn(
            "Cannot find valid value for arg ["
                + key
                + "] from settings : "
                + e.getClass().getName());
        return null;
      } finally {
        byteBuffer.rewind(); // cast for JDK 8- runtime compatibility
      }
    }

    @Override
    public Boolean readValue(Object object) {
      if (object instanceof Boolean) {
        return (Boolean) object;
      }

      return null;
    }

    @Override
    public ByteBuffer toByteBuffer(Boolean value) {
      if (value != null) {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(value ? 1 : 0);

        buffer.rewind();

        return buffer;
      } else {
        return null;
      }
    }
  }

  public static class ByteArraySettingsArg extends SettingsArg<byte[]> {
    ByteArraySettingsArg(String key) {
      super(key);
    }

    @Override
    public byte[] readValue(ByteBuffer byteBuffer) {
      try {
        byte[] value = new byte[byteBuffer.remaining()];
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).get(value);
        return value;
      } catch (BufferUnderflowException e) {
        logger.warn(
            "Cannot find valid value for arg ["
                + key
                + "] from settings : "
                + e.getClass().getName());
        return null;
      } finally {
        byteBuffer.rewind();
      }
    }

    @Override
    public byte[] readValue(Object object) {
      if (object instanceof byte[]) {
        return (byte[]) object;

      } else if (object instanceof String) {
        return ((String) object).getBytes();
      }

      return null;
    }

    @Override
    public ByteBuffer toByteBuffer(byte[] value) {
      if (value != null) {
        ByteBuffer buffer = ByteBuffer.allocate(value.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(value);

        buffer.rewind();

        return buffer;
      } else {
        return null;
      }
    }

    @Override
    public boolean areValuesEqual(byte[] value1, byte[] value2) {
      return Arrays.equals(value1, value2);
    }
  }
}
