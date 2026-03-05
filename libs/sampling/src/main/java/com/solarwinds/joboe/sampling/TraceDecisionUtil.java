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
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;

public class TraceDecisionUtil {
  public static final int SAMPLE_RESOLUTION = 1000000;
  private static final Logger logger = LoggerFactory.getLogger();

  // Used to control the rate and max traces can be generated
  private static final ConcurrentHashMap<TokenBucketType, TokenBucket> tokenBuckets =
      new ConcurrentHashMap<TokenBucketType, TokenBucket>();

  private static final Random rand = new Random();

  private static final Map<MetricType, AtomicInteger> requestCounters =
      new HashMap<MetricType, AtomicInteger>();

  private static final Map<String, TraceConfig> lastTraceConfigs =
      new ConcurrentHashMap<String, TraceConfig>();

  // map of flags to keep of whether a settings error was reported, only report it once
  private static final ConcurrentHashMap<String, Boolean> reportedSettingsError =
      new ConcurrentHashMap<String, Boolean>();

  static {
    for (MetricType type : MetricType.values()) {
      requestCounters.put(type, new AtomicInteger(0));
    }
  }

  /**
   * Determines if we should trace this request.
   *
   * @param layer
   * @param inXTraceID incoming validated X-Trace-Id, if it is not valid, it should be null
   * @param xTraceOptions incoming X-Trace-Options, null if not defined
   * @param signals resource if the request has one, for example URL for web request, job name for
   *     jobs
   * @return trace decision, should always be nonnull
   */
  public static TraceDecision shouldTraceRequest(
      String layer, String inXTraceID, XtraceOptions xTraceOptions, List<String> signals) {
    boolean isTriggerTrace;
    try {
      RequestType requestType = getRequestType(inXTraceID, xTraceOptions);
      isTriggerTrace = requestType.isTriggerTrace();

      // First get the config from remote source (SRv1)
      TraceConfig remoteConfig = getRemoteTraceConfig();

      if (remoteConfig == null) { // settings not readable, do not trace
        logger.debug(
            "Failed to fetch settings records, not tracing"); // debug message, otherwise it could
        // be very noisy if remote config is
        // not found
        return new TraceDecision(false, false, null, requestType);
      }

      // Get the local sample rate (either from file, defaults, JVM, url matching etc)
      TraceConfig localConfig = getLocalTraceConfig(signals);

      TraceConfig config =
          computeTraceConfig(
              remoteConfig,
              localConfig,
              SettingsManager.getSamplingConfiguration().isTriggerTraceEnabled());

      if (!config.isSampleRateConfigured()) {
        logger.debug(
            "Cannot trace request as sample rate is undefined"); // debug message, otherwise it
        // could be very noisy
        return new TraceDecision(false, false, config, requestType);
      }

      if (!config.isFlagsConfigured()) {
        logger.debug(
            "Cannot trace request as flags are undefined"); // debug message, otherwise it could be
        // very noisy
        return new TraceDecision(false, false, config, requestType);
      }

      boolean isReportMetrics = isReportMetricsByConfig(config);
      if (xTraceOptions != null && xTraceOptions.getAuthenticationStatus().isFailure()) {
        logger.debug("Bad x-tv-options-signature, not tracing");
        return new TraceDecision(false, isReportMetrics, null, requestType);
      }

      if (config
          .hasSampleStartFlag()) { // only makes sense to record the sample rate if start flag is
        // ON. as it is the only case that the sample rate is actually
        // used
        recordLastTraceConfig(config, layer);
      }

      Metadata inMetadata = null;
      if (inXTraceID != null) {
        inMetadata = validateMetadata(inXTraceID);
      }
      boolean isSampled = isSampledByConfig(inMetadata, config, isTriggerTrace);
      boolean bucketExhausted = false;

      // perform token bucket check if it is a new trace
      if (isSampled) {
        if (inMetadata == null) {
          TokenBucket tokenBucket =
              getTokenBucket(
                  requestType.bucketType,
                  remoteConfig.getBucketCapacity(requestType.bucketType),
                  remoteConfig.getBucketRate(
                      requestType
                          .bucketType)); // passes in remoteConfig as this is the only source of
          // token bucket parameters
          if (tokenBucket.consume()) { // check whether there are tokens left to be consumed
            incrementMetrics(MetricType.TRACE_COUNT); // count all new traffic that will be traced
            if (requestType.isTriggerTrace()) {
              incrementMetrics(MetricType.TRIGGERED_TRACE_COUNT);
            }
          } else {
            logger.trace("No Tokens available in the Token Bucket. Not tracing this request");

            incrementMetrics(MetricType.TOKEN_BUCKET_EXHAUSTION);
            isSampled = false; // flip it to false due to exhausted bucket
            bucketExhausted = true;
          }
        } else {
          incrementMetrics(MetricType.TRACE_COUNT); // count all through traffic that will be traced
        }
      }

      return new TraceDecision(
          isSampled, isReportMetrics, bucketExhausted, config, requestType, inMetadata);
    } finally {
      incrementMetrics(MetricType.THROUGHPUT);
    }
  }

  private static Metadata validateMetadata(String inXTraceID) {
    if (!Metadata.isCompatible(inXTraceID)) {
      logger.debug("Not accepting X-Trace ID [" + inXTraceID + "] for trace continuation");
      return null;
    }

    try {
      Metadata inMetadata = new Metadata(inXTraceID);
      if (!inMetadata.isValid()) {
        logger.debug("Invalid incoming x-trace ID [" + inXTraceID + "]");
        return null;
      }
      return inMetadata;
    } catch (SamplingException e) {
      logger.warn("Failed to parse x-trace ID [" + inXTraceID + "] " + e.getMessage());
      return null;
    }
  }

  private static Map.Entry<String, Object> getTagEntry(String key, Object value) {
    return new AbstractMap.SimpleEntry<>(key, value);
  }

  private static RequestType getRequestType(String inXTraceId, XtraceOptions xTraceOptions) {
    if (xTraceOptions == null
        || inXTraceId
            != null) { // if there's an incoming valid x-trace ID then trigger trace option is
      // ignored
      return RequestType.REGULAR;
    }
    RequestType requestType;
    XtraceOptions.AuthenticationStatus authenticationStatus =
        xTraceOptions.getAuthenticationStatus();
    if (authenticationStatus.isFailure()) {
      requestType = RequestType.BAD_SIGNATURE;
    } else if (!xTraceOptions.getOptionValue(XtraceOption.TRIGGER_TRACE)) {
      requestType = RequestType.REGULAR;
    } else if (authenticationStatus.isAuthenticated()) {
      requestType = RequestType.AUTHENTICATED_TRIGGER_TRACE;
    } else {
      requestType = RequestType.UNAUTHENTICATED_TRIGGER_TRACE;
    }
    return requestType;
  }

  /**
   * Retrieves the token bucket based on requestType. Update the bucket with newBucketCapacity and
   * newBucketRate
   *
   * @param bucketType - token bucket type
   * @param newBucketCapacity new Bucket capacity
   * @param newBucketRate new Bucket replenish rate
   * @return token bucket of the given layer updated with the newBucketCapacity and newBucketRate
   *     provided
   */
  static TokenBucket getTokenBucket(
      TokenBucketType bucketType, double newBucketCapacity, double newBucketRate) {
    TokenBucket bucket = tokenBuckets.get(bucketType);
    if (bucket == null) {
      bucket = new TokenBucket(newBucketCapacity, newBucketRate);
      tokenBuckets.put(bucketType, bucket);
    } else {
      bucket.setCapacity(newBucketCapacity);
      bucket.setRatePerSecond(newBucketRate);
    }

    return bucket;
  }

  /**
   * Retrieves Trace Config from a remote source
   *
   * @return TraceConfig if a settings is found, null if the settings cannot be found/ not yet
   *     available
   */
  public static TraceConfig getRemoteTraceConfig() {
    TraceConfig config = null;
    Settings settings = SettingsManager.getSettings();

    if (settings != null) {
      Map<TokenBucketType, Double> bucketCapacities =
          getBucketSettings(
              settings,
              SettingsArg.BUCKET_CAPACITY,
              SettingsArg.RELAXED_BUCKET_CAPACITY,
              SettingsArg.STRICT_BUCKET_CAPACITY);
      Map<TokenBucketType, Double> bucketRates =
          getBucketSettings(
              settings,
              SettingsArg.BUCKET_RATE,
              SettingsArg.RELAXED_BUCKET_RATE,
              SettingsArg.STRICT_BUCKET_RATE);

      config =
          new TraceConfig(
              (int) settings.getValue(),
              settings.isDefault() ? SampleRateSource.OBOE_DEFAULT : SampleRateSource.OBOE,
              settings.getFlags(),
              bucketCapacities,
              bucketRates);
    }

    return config;
  }

  private static Map<TokenBucketType, Double> getBucketSettings(
      Settings settings,
      SettingsArg<Double> regularBucketArg,
      SettingsArg<Double> relaxedBucketArg,
      SettingsArg<Double> strictBucketArg) {
    Map<TokenBucketType, Double> result = new HashMap<TokenBucketType, Double>();
    result.put(TokenBucketType.REGULAR, getBucketSettingsArg(settings, regularBucketArg));
    result.put(TokenBucketType.RELAXED, getBucketSettingsArg(settings, relaxedBucketArg));
    result.put(TokenBucketType.STRICT, getBucketSettingsArg(settings, strictBucketArg));

    return result;
  }

  /**
   * Validates the bucket settings, if it's invalid, 0 will be returned
   *
   * @param settings
   * @param arg
   * @return
   */
  private static double getBucketSettingsArg(Settings settings, SettingsArg<Double> arg) {
    Double value = settings.getArgValue(arg);
    if (value == null) {
      logger.debug("Cannot read settings arg " + arg);
      return 0;
    } else if (value < 0) {
      logger.warn("Invalid negative value in settings arg " + arg);
      return 0;
    }

    return value;
  }

  /**
   * Gets the Trace Config from local system based on the URL (if available) of the request
   *
   * @param signals URL of a web based request, null otherwise
   * @return A URL specific Trace Config will be returned if a match is found, otherwise the
   *     universal Trace Config from local settings.
   */
  public static TraceConfig getLocalTraceConfig(List<String> signals) {
    TraceConfig localTraceConfig = getLocalSpecificTraceConfig(signals);
    if (localTraceConfig == null) {
      localTraceConfig = getLocalUniversalTraceConfig();
    }

    return localTraceConfig;
  }

  /**
   * Retrieves universal (general settings that apply to the java process) Trace Config from local
   * Settings such as jvm arguments and java agent config file
   *
   * @return
   */
  private static TraceConfig getLocalUniversalTraceConfig() {
    Integer sampleRate = SettingsManager.getSamplingConfiguration().getSampleRate();
    TracingMode tracingMode = SettingsManager.getSamplingConfiguration().getTracingMode();
    return new TraceConfig(
        sampleRate,
        sampleRate != null ? SampleRateSource.FILE : SampleRateSource.DEFAULT,
        tracingMode != null ? tracingMode.toFlags() : null);
  }

  /**
   * Retrieves Resource specific Trace Config from local settings (by URL, job names etc)
   *
   * @param signals URL of the web request, null otherwise
   * @return Resource specific Trace Config if a match is found from local settings, otherwise null
   *     is returned
   */
  private static TraceConfig getLocalSpecificTraceConfig(List<String> signals) {
    if (signals != null
        && SettingsManager.getSamplingConfiguration().getInternalTransactionSettings() != null) {
      return SettingsManager.getSamplingConfiguration()
          .getInternalTransactionSettings()
          .getTraceConfig(signals);
    } else {
      return null;
    }
  }

  /**
   * Computes the trace config based on the precedence list.
   *
   * @param remoteConfig should not be null
   * @param localConfig
   * @return
   */
  static TraceConfig computeTraceConfig(
      TraceConfig remoteConfig, TraceConfig localConfig, boolean localTriggerTraceEnabled) {
    boolean hasRemoteConfigOverride = remoteConfig.hasOverrideFlag();

    // consider sample rate:
    TraceConfig sampleRateConfig;

    if (hasRemoteConfigOverride && localConfig.isSampleRateConfigured()) {
      sampleRateConfig =
          remoteConfig.getSampleRate() <= localConfig.getSampleRate() ? remoteConfig : localConfig;
    } else if (localConfig.isSampleRateConfigured()) {
      sampleRateConfig = localConfig;
    } else {
      sampleRateConfig = remoteConfig;
    }

    Integer sampleRate = sampleRateConfig.getSampleRate();
    SampleRateSource sampleRateSource = sampleRateConfig.getSampleRateSource();

    // consider tracing flags:
    short flags;
    if (hasRemoteConfigOverride && localConfig.isFlagsConfigured()) { // get the Lower flags
      flags = (short) (remoteConfig.getFlags() & localConfig.getFlags());
    } else if (localConfig.isFlagsConfigured()) {
      flags = localConfig.getFlags();
    } else {
      flags = remoteConfig.getFlags();
    }

    // special case : consider trigger trace local config if explicitly disabled
    if (!localTriggerTraceEnabled) {
      flags = (short) (flags & ~Settings.OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED); // disable the
      // OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED bit
    }

    return new TraceConfig(
        sampleRate,
        sampleRateSource,
        flags,
        remoteConfig.getBucketCapacities(),
        remoteConfig.getBucketRates()); // token bucket parameters are always from remote config
  }

  private static boolean sampled(int sampleRate) {
    return (sampleRate == SAMPLE_RESOLUTION
        || (sampleRate < SAMPLE_RESOLUTION && rand.nextInt(SAMPLE_RESOLUTION) <= sampleRate));
  }

  /**
   * Checks whether we should sample based on the TraceConfig and various criteria
   *
   * @param inMetadata
   * @param config
   * @return whether we should trace
   */
  private static boolean isSampledByConfig(
      Metadata inMetadata, TraceConfig config, boolean isTriggerTrace) {
    if (inMetadata == null) { // new trace
      if (isTriggerTrace) { // trigger trace only matters for new traces
        return config.hasSampleTriggerTraceFlag();
      } else {
        if (config.hasSampleStartFlag()) {
          incrementMetrics(MetricType.SAMPLE_COUNT);
          return sampled(config.getSampleRate());
        } else {
          return false;
        }
      }
    } else { // continue trace
      Metadata continuingMetadata = inMetadata;

      if (!continuingMetadata
          .isSampled()) { // sampling decision from previous service decides not to trace
        return false;
      } else // this flag is not being used currently
      if (config.hasSampleThroughAlwaysFlag()) {
        incrementMetrics(MetricType.THROUGH_TRACE_COUNT);
        return true;
      } else return config.hasSampleThroughFlag() && sampled(config.getSampleRate());
    }
  }

  private static boolean isReportMetricsByConfig(TraceConfig config) {
    return config
        .isMetricsEnabled(); // report metrics as far as the metrics is enabled in config. No
    // propagation for now
  }

  /**
   * Increments the count for the given metricType.
   *
   * @param metricType
   */
  private static void incrementMetrics(MetricType metricType) {
    requestCounters.get(metricType).incrementAndGet();
  }

  /**
   * Records the last accessed sample rate for metric reporting functionality
   *
   * @param config last accessed sample rate
   * @param layer
   */
  private static void recordLastTraceConfig(TraceConfig config, String layer) {
    synchronized (lastTraceConfigs) {
      lastTraceConfigs.put(layer, config);
    }
  }

  /**
   * Consumes and return a clone of metrics data map of the Metric type as argument. Take note of
   * the side effect that, after the call, the existing metric data would be consumed and cleared
   *
   * @param type Metric type
   * @return a clone of the metric data map of the Metric type, null if the Metric is not available
   *     for that type
   */
  public static int consumeMetricsData(MetricType type) {
    AtomicInteger metricData = requestCounters.get(type);
    return metricData.getAndSet(0);
  }

  /**
   * Consumes and return a clone of map of last accessed sample rate settings. Take note of the side
   * effect that, after the call, the existing last accessed sampler ate settings would be consumed
   * and cleared
   *
   * @return a lone of map of last accessed sample rate settings
   */
  public static Map<String, TraceConfig> consumeLastTraceConfigs() {
    Map<String, TraceConfig> configs;
    synchronized (lastTraceConfigs) {
      configs = new HashMap<String, TraceConfig>(lastTraceConfigs);
      lastTraceConfigs.clear();
    }

    return configs;
  }

  /** For internal testing usage only */
  public static void reset() {
    tokenBuckets.clear();

    for (AtomicInteger counter : requestCounters.values()) {
      counter.set(0);
    }
    lastTraceConfigs.clear();
    reportedSettingsError.clear();
  }

  public enum MetricType {
    THROUGHPUT,
    TOKEN_BUCKET_EXHAUSTION,
    TRACE_COUNT,
    SAMPLE_COUNT,
    THROUGH_TRACE_COUNT,
    TRIGGERED_TRACE_COUNT // ,SIGNED_REQUEST_COUNT
  }

  @Getter
  public enum RequestType {
    REGULAR(false, TokenBucketType.REGULAR),
    AUTHENTICATED_TRIGGER_TRACE(true, TokenBucketType.RELAXED),
    UNAUTHENTICATED_TRIGGER_TRACE(true, TokenBucketType.STRICT),
    BAD_SIGNATURE(false, TokenBucketType.REGULAR);

    private final boolean triggerTrace;
    private final TokenBucketType bucketType;

    RequestType(boolean triggerTrace, TokenBucketType bucketType) {
      this.triggerTrace = triggerTrace;
      this.bucketType = bucketType;
    }
  }
}
