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
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * An option key within X-Trace-Options header. This does NOT store the option value
 *
 * @param <V> The value type of the option
 * @see XtraceOptions
 */
@Getter
public class XtraceOption<V> {

  private static final Logger LOGGER = LoggerFactory.getLogger();
  private static final Map<String, XtraceOption<?>> keyLookup =
      new HashMap<String, XtraceOption<?>>();
  public static final XtraceOption<Boolean> TRIGGER_TRACE =
      new XtraceOption<Boolean>("trigger-trace", null, false);
  public static final XtraceOption<String> SW_KEYS =
      new XtraceOption<String>("sw-keys", ValueParser.STRING_VALUE_PARSER);
  public static final XtraceOption<Long> TS =
      new XtraceOption<Long>("ts", ValueParser.LONG_VALUE_PARSER);
  public static final String CUSTOM_KV_PREFIX = "custom-";

  private final V defaultValue;
  @Getter private final String key;
  private final ValueParser<V> parser;
  private boolean isCustomKv = false;

  /**
   * @param key
   * @param parser null parser indicates that this is a key only option
   */
  private XtraceOption(String key, ValueParser<V> parser) {
    this(key, parser, null);
  }

  private XtraceOption(String key, ValueParser<V> parser, V defaultValue) {
    this.key = key;
    this.defaultValue = defaultValue;
    this.parser = parser;

    keyLookup.put(key, this);
  }

  public static XtraceOption<?> fromKey(String key) {
    if (key.contains(
        " ")) { // invalid key if it contains any space. Not using regex here as it could be pretty
      // slow
      return null;
    }

    XtraceOption<?> option = keyLookup.get(key);
    if (option != null) {
      return option;
    } else if (isCustomKv(key)) {
      option = new XtraceOption<>(key, ValueParser.STRING_VALUE_PARSER);
      option.isCustomKv = true;
      return option;
    } else {
      return null;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    XtraceOption<?> that = (XtraceOption<?>) o;

    return key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  /**
   * Whether this option is a custom one that starts with {@link XtraceOption#CUSTOM_KV_PREFIX}
   *
   * @return
   */
  public boolean isCustomKv() {
    return this.isCustomKv;
  }

  /**
   * Whether this option should appear in key-value pair or not.
   *
   * @return
   */
  public boolean isKeyOnlyOption() {
    return parser == null;
  }

  private static boolean isCustomKv(String key) {
    return key.startsWith(CUSTOM_KV_PREFIX);
  }

  private interface ValueParser<V> {
    V parse(String stringValue) throws IllegalArgumentException;

    BooleanValueParser BOOLEAN_VALUE_PARSER = new BooleanValueParser();
    StringValueParser STRING_VALUE_PARSER = new StringValueParser();
    LongValueParser LONG_VALUE_PARSER = new LongValueParser();
  }

  public V parseValueFromString(String value)
      throws XtraceOptions.InvalidValueXTraceOptionException {
    try {
      return parser != null ? parser.parse(value) : null;
    } catch (IllegalArgumentException e) {
      throw new XtraceOptions.InvalidValueXTraceOptionException(this, value);
    }
  }

  private static class BooleanValueParser implements ValueParser<Boolean> {
    @Override
    public Boolean parse(String stringValue) {
      return "1".equals(stringValue) || Boolean.valueOf(stringValue);
    }
  }

  private static class StringValueParser implements ValueParser<String> {
    @Override
    public String parse(String stringValue) {
      return stringValue;
    }
  }

  private static class LongValueParser implements ValueParser<Long> {
    @Override
    public Long parse(String stringValue) throws NumberFormatException {
      return Long.parseLong(stringValue);
    }
  }
}
