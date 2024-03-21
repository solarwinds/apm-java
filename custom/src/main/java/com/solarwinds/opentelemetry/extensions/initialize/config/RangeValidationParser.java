package com.solarwinds.opentelemetry.extensions.initialize.config;

import com.solarwinds.joboe.config.ConfigParser;
import com.solarwinds.joboe.config.InvalidConfigException;

public class RangeValidationParser<T extends Comparable<T>> implements ConfigParser<T, T> {
  private final T min;
  private final T max;
  private final boolean minInclusive;
  private final boolean maxInclusive;

  public RangeValidationParser(T min, T max) {
    this(min, max, true, true);
  }

  public RangeValidationParser(T min, T max, boolean minInclusive, boolean maxInclusive) {
    this.min = min;
    this.max = max;
    this.minInclusive = minInclusive;
    this.maxInclusive = maxInclusive;
  }

  public boolean isInRange(T value) {
    if (minInclusive) {
      if (value.compareTo(min) < 0) {
        return false;
      }
    } else {
      if (value.compareTo(min) <= 0) {
        return false;
      }
    }

    if (maxInclusive) {
      return value.compareTo(max) <= 0;
    } else {
      return value.compareTo(max) < 0;
    }
  }

  @Override
  public T convert(T value) throws InvalidConfigException {
    if (!isInRange(value)) {
      throw new InvalidConfigException(
          "Value ["
              + value
              + "] is not within the range of "
              + (minInclusive ? '[' : ')')
              + min
              + ", "
              + max
              + (maxInclusive ? ']' : ')'));
    }
    return value;
  }
}
