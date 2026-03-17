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

import java.util.Collections;
import java.util.Map;
import lombok.Getter;

/**
 * Sample Rate Configuration
 *
 * @see TraceDecisionUtil
 */
public class TraceConfig {
  private final Integer sampleRate;
  @Getter private final SampleRateSource sampleRateSource;
  private final Short flags;
  @Getter private final Map<TokenBucketType, Double> bucketCapacities;
  @Getter private final Map<TokenBucketType, Double> bucketRates;

  public TraceConfig(Integer sampleRate, SampleRateSource sampleRateSource, Short flags) {
    this(sampleRate, sampleRateSource, flags, Collections.emptyMap(), Collections.emptyMap());
  }

  public TraceConfig(
      Integer sampleRate,
      SampleRateSource sampleRateSource,
      Short flags,
      Map<TokenBucketType, Double> bucketCapacities,
      Map<TokenBucketType, Double> bucketRates) {
    this.sampleRate = sampleRate;
    this.sampleRateSource = sampleRateSource;
    this.flags = flags;
    this.bucketCapacities = bucketCapacities;
    this.bucketRates = bucketRates;
  }

  public int getSampleRate() {
    return sampleRate;
  }

  public int getSampleRateSourceValue() {
    return sampleRateSource.value();
  }

  public double getBucketCapacity(TokenBucketType bucketType) {
    return bucketCapacities.containsKey(bucketType) ? bucketCapacities.get(bucketType) : 0;
  }

  public double getBucketRate(TokenBucketType bucketType) {
    return bucketRates.containsKey(bucketType) ? bucketRates.get(bucketType) : 0;
  }

  public boolean hasOverrideFlag() {
    return flags != null
        && (flags & Settings.OBOE_SETTINGS_FLAG_OVERRIDE) == Settings.OBOE_SETTINGS_FLAG_OVERRIDE;
  }

  public boolean hasSampleStartFlag() {
    return flags != null
        && (flags & Settings.OBOE_SETTINGS_FLAG_SAMPLE_START)
            == Settings.OBOE_SETTINGS_FLAG_SAMPLE_START;
  }

  public boolean hasSampleThroughFlag() {
    return flags != null
        && (flags & Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH)
            == Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH;
  }

  public boolean hasSampleThroughAlwaysFlag() {
    return flags != null
        && (flags & Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS)
            == Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS;
  }

  public boolean isMetricsEnabled() {
    return flags != null
        && (flags
                & (Settings.OBOE_SETTINGS_FLAG_SAMPLE_START
                    | Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS))
            != 0; // for now if those 2 flags are not on, we assume it's metrics disabled
  }

  public boolean hasSampleTriggerTraceFlag() {
    return (flags & Settings.OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED)
        == Settings.OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED;
  }

  short getFlags() {
    return flags;
  }

  public boolean isFlagsConfigured() {
    return flags != null;
  }

  public boolean isSampleRateConfigured() {
    return sampleRate != null;
  }

  @Override
  public String toString() {
    return "SampleRateConfig [sampleRate="
        + sampleRate
        + ", sampleRateSource="
        + sampleRateSource
        + ", flags="
        + flags
        + ", bucketCapacities="
        + bucketCapacities
        + ", bucketRates="
        + bucketRates
        + "]";
  }
}
