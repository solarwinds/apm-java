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

package com.solarwinds.joboe.core.rpc;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.joboe.sampling.SettingsArg;
import com.solarwinds.joboe.sampling.TraceDecisionUtil;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/** Settings extracted from RPC calls */
public class RpcSettings extends com.solarwinds.joboe.sampling.Settings {
  private static final Logger logger = LoggerFactory.getLogger();

  private final short type; // required
  private final short flags; // required
  private final long timestamp; // required, in millsec
  private final long value; // required
  private final long ttl; // time to live this settings record
  private final Map<SettingsArg<?>, Object> args =
      new HashMap<SettingsArg<?>, Object>(); // other arguments

  public RpcSettings(
      String stringFlags, long timestamp, long value, long ttl, Map<String, ByteBuffer> args) {
    this.type = Settings.OBOE_SETTINGS_TYPE_DEFAULT_SAMPLE_RATE;
    this.flags = convertFlagsFromStringToShort(stringFlags);
    this.timestamp = timestamp;
    if (value < 0) {
      logger.warn(
          "Received invalid sample rate from RPC client : "
              + value
              + ", must be between 0 and "
              + TraceDecisionUtil.SAMPLE_RESOLUTION);
      this.value = 0;
    } else if (value > TraceDecisionUtil.SAMPLE_RESOLUTION) {
      logger.warn(
          "Received invalid sample rate from RPC client : "
              + value
              + ", must be between 0 and "
              + TraceDecisionUtil.SAMPLE_RESOLUTION);
      this.value = TraceDecisionUtil.SAMPLE_RESOLUTION;
    } else {
      this.value = value;
    }
    this.ttl = ttl;
    readArgs(args);
  }

  /**
   * For internal testing purpose only
   *
   * @param source
   * @param timestamp a new timestamp
   */
  public RpcSettings(RpcSettings source, long timestamp) {
    this.type = source.type;
    this.flags = source.flags;
    this.timestamp = timestamp; // take the new timestamp
    this.value = source.value;
    this.ttl = source.ttl;
    this.args.putAll(source.args);
  }

  /**
   * Read arguments from the input map
   *
   * @param inputArgs
   */
  private void readArgs(Map<String, ByteBuffer> inputArgs) {
    for (Entry<String, ByteBuffer> inputArg : inputArgs.entrySet()) {
      SettingsArg<?> arg = SettingsArg.fromKey(inputArg.getKey());
      if (arg == null) {
        logger.debug("Cannot recognize argument [" + inputArg.getKey() + "], ignoring...");
      } else {
        args.put(arg, arg.readValue(inputArg.getValue()));
      }
    }
  }

  public static short convertFlagsFromStringToShort(String stringFlags) {
    short flags = 0;
    String[] flagTokens = stringFlags.split(",");
    for (String flagToken : flagTokens) {
      if ("OVERRIDE".equals(flagToken)) {
        flags |= OBOE_SETTINGS_FLAG_OVERRIDE;
      } else if ("SAMPLE_START".equals(flagToken)) {
        flags |= OBOE_SETTINGS_FLAG_SAMPLE_START;
      } else if ("SAMPLE_THROUGH".equals(flagToken)) {
        flags |= OBOE_SETTINGS_FLAG_SAMPLE_THROUGH;
      } else if ("SAMPLE_THROUGH_ALWAYS".equals(flagToken)) {
        flags |= OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS;
      } else if ("TRIGGER_TRACE".equals(flagToken)) {
        flags |= OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED;
      } else if ("SAMPLE_BUCKET_ENABLED".equals(flagToken)) { // not used anymore
        flags |= OBOE_SETTINGS_FLAG_SAMPLE_BUCKET_ENABLED;
      } else {
        logger.debug("Unknown flag found from settings: " + flagToken);
      }
    }
    return flags;
  }

  @Override
  public long getValue() {
    return value;
  }

  @Override
  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public short getType() {
    return type;
  }

  @Override
  public short getFlags() {
    return flags;
  }

  @Override
  public long getTtl() {
    return ttl;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getArgValue(SettingsArg<T> arg) {
    return (T) args.get(arg);
  }
}
