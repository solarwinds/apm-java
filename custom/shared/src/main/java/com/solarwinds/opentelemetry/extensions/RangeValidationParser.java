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
