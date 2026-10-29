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

package com.solarwinds.opentelemetry.extensions.config.provider;

import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.ResourceMatcher;
import com.solarwinds.joboe.sampling.SampleRateSource;
import com.solarwinds.joboe.sampling.SamplingConfiguration;
import com.solarwinds.joboe.sampling.SettingsFetcher;
import com.solarwinds.joboe.sampling.SettingsManager;
import com.solarwinds.joboe.sampling.TraceConfig;
import com.solarwinds.joboe.sampling.TraceConfigs;
import com.solarwinds.joboe.sampling.TracingMode;
import com.solarwinds.opentelemetry.extensions.SolarwindsSampler;
import com.solarwinds.opentelemetry.extensions.config.parser.json.ResourceExtensionsMatcher;
import com.solarwinds.opentelemetry.extensions.config.parser.json.StringPatternMatcher;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public abstract class AbstractSamplerComponentProvider implements ComponentProvider {
  private static final Logger logger = LoggerFactory.getLogger();

  private static final String TRACING_ENABLED = "tracing_enabled";

  private static final String TRIGGER_TRACE_ENABLED = "trigger_trace_enabled";

  private static final String TRANSACTION_SETTINGS = "transaction_settings";

  private static final String URL_SAMPLE_RATES = "url_sample_rates";

  private static final String TRACING = "tracing";

  private static final String REGEX = "regex";

  private static final String EXTENSIONS = "extensions";

  private static final String SAMPLE_RATE = "sample_rate";

  private static final List<String> TRANSACTION_SETTING_KEYS =
      Arrays.asList(TRACING, REGEX, EXTENSIONS);

  @Override
  public Class<Sampler> getType() {
    return Sampler.class;
  }

  @Override
  public Sampler create(DeclarativeConfigProperties declarativeConfigProperties) {
    try {
      SamplingConfiguration.SamplingConfigurationBuilder builder =
          SamplingConfiguration.builder()
              .tracingMode(parseTracingMode(declarativeConfigProperties))
              .triggerTraceEnabled(
                  declarativeConfigProperties.getBoolean(TRIGGER_TRACE_ENABLED, true))
              .internalTransactionSettings(parseTraceConfigs(declarativeConfigProperties));

      parseConfig(declarativeConfigProperties, builder);
      SettingsManager.initialize(
          createSettingsFetcher(declarativeConfigProperties), builder.build());

    } catch (InvalidConfigException e) {
      throw new RuntimeException(e);
    }

    return new SolarwindsSampler();
  }

  protected void parseConfig(
      DeclarativeConfigProperties properties,
      SamplingConfiguration.SamplingConfigurationBuilder builder)
      throws InvalidConfigException {}

  protected abstract SettingsFetcher createSettingsFetcher(DeclarativeConfigProperties properties)
      throws InvalidConfigException;

  private TracingMode parseTracingMode(DeclarativeConfigProperties properties) {
    Boolean tracingEnabled = properties.getBoolean(TRACING_ENABLED);
    if (tracingEnabled == null) {
      return null;
    }
    return tracingEnabled ? TracingMode.ENABLED : TracingMode.DISABLED;
  }

  private TraceConfigs parseTraceConfigs(DeclarativeConfigProperties properties)
      throws InvalidConfigException {
    List<DeclarativeConfigProperties> transactionSettings =
        properties.getStructuredList(TRANSACTION_SETTINGS);
    List<DeclarativeConfigProperties> urlSampleRates =
        properties.getStructuredList(URL_SAMPLE_RATES);

    if (transactionSettings != null) {
      if (urlSampleRates != null) {
        logger.warn(
            URL_SAMPLE_RATES + " is ignored as " + TRANSACTION_SETTINGS + " is also defined");
      }
      return parseTransactionSettings(transactionSettings);
    }

    if (urlSampleRates != null) {
      return parseUrlSampleRates(urlSampleRates);
    }
    return null;
  }

  private TraceConfigs parseTransactionSettings(List<DeclarativeConfigProperties> settings)
      throws InvalidConfigException {
    Map<ResourceMatcher, TraceConfig> result = new LinkedHashMap<>();
    for (DeclarativeConfigProperties setting : settings) {
      result.put(parseMatcher(setting), parseTransactionTraceConfig(setting));
    }
    return new TraceConfigs(result);
  }

  private ResourceMatcher parseMatcher(DeclarativeConfigProperties setting)
      throws InvalidConfigException {
    Set<String> unknownKeys = new HashSet<>(setting.getPropertyKeys());
    TRANSACTION_SETTING_KEYS.forEach(unknownKeys::remove);
    if (!unknownKeys.isEmpty()) {
      throw new InvalidConfigException(
          "Failed to parse \""
              + TRANSACTION_SETTINGS
              + "\". Unknown key(s) "
              + unknownKeys
              + " found");
    }

    String regex = setting.getString(REGEX);
    List<String> extensions = setting.getScalarList(EXTENSIONS, String.class);

    if (regex != null && extensions != null) {
      throw new InvalidConfigException(
          "Multiple matchers found for \"" + TRANSACTION_SETTINGS + "\" entry " + setting);
    } else if (regex != null) {
      try {
        return new StringPatternMatcher(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
      } catch (PatternSyntaxException e) {
        throw new InvalidConfigException(
            "Failed to compile pattern "
                + regex
                + " defined in \""
                + TRANSACTION_SETTINGS
                + "."
                + REGEX
                + "\", error message ["
                + e.getMessage()
                + "].",
            e);
      }
    } else if (extensions != null) {
      return new ResourceExtensionsMatcher(new HashSet<>(extensions));
    } else {
      throw new InvalidConfigException(
          "Cannot find proper matcher for \""
              + TRANSACTION_SETTINGS
              + "\" entry "
              + setting
              + ". Neither "
              + REGEX
              + " nor "
              + EXTENSIONS
              + " was defined");
    }
  }

  private TraceConfig parseTransactionTraceConfig(DeclarativeConfigProperties setting)
      throws InvalidConfigException {
    String tracingModeString = setting.getString(TRACING);
    if (tracingModeString == null) {
      throw new InvalidConfigException(
          "Need to define \"" + TRACING + "\" for each entry in \"" + TRANSACTION_SETTINGS + "\"");
    }

    TracingMode tracingMode = TracingMode.fromString(tracingModeString);
    if (tracingMode == null) {
      throw new InvalidConfigException(
          "Invalid \""
              + TRACING
              + "\" value ["
              + tracingModeString
              + "], must either be "
              + TracingMode.ENABLED.getStringValue()
              + " or "
              + TracingMode.DISABLED.getStringValue());
    }

    if (tracingMode == TracingMode.ALWAYS || tracingMode == TracingMode.ENABLED) {
      // undefined sample rate if trace mode is enabled
      return new TraceConfig(null, SampleRateSource.FILE, tracingMode.toFlags());
    }
    return new TraceConfig(0, SampleRateSource.FILE, tracingMode.toFlags());
  }

  private TraceConfigs parseUrlSampleRates(List<DeclarativeConfigProperties> urlSampleRates)
      throws InvalidConfigException {
    Map<ResourceMatcher, TraceConfig> result = new LinkedHashMap<>();
    for (DeclarativeConfigProperties urlSampleRate : urlSampleRates) {
      Set<String> propertyKeys = urlSampleRate.getPropertyKeys();
      if (propertyKeys.size() != 1) {
        throw new InvalidConfigException(
            "Invalid \"" + URL_SAMPLE_RATES + "\" entry " + urlSampleRate);
      }

      String url = propertyKeys.iterator().next();
      Pattern pattern;
      try {
        pattern = Pattern.compile(url, Pattern.CASE_INSENSITIVE);
      } catch (PatternSyntaxException e) {
        throw new InvalidConfigException(
            "Failed to compile the url sample rate of url pattern ["
                + url
                + "], error message ["
                + e.getMessage()
                + "].",
            e);
      }

      DeclarativeConfigProperties urlConfig =
          urlSampleRate.getStructured(url, DeclarativeConfigProperties.empty());
      result.put(new StringPatternMatcher(pattern), parseUrlTraceConfig(urlConfig, url));
    }
    return new TraceConfigs(result);
  }

  private TraceConfig parseUrlTraceConfig(DeclarativeConfigProperties urlConfig, String url)
      throws InvalidConfigException {
    Integer sampleRate = urlConfig.getInt(SAMPLE_RATE);
    Boolean tracingEnabled = urlConfig.getBoolean(TRACING_ENABLED);

    if (sampleRate == null && tracingEnabled == null) {
      throw new InvalidConfigException(
          "Need to define either "
              + TRACING_ENABLED
              + " or "
              + SAMPLE_RATE
              + ", but found none in ["
              + url
              + "]");
    }

    if (sampleRate == null) {
      if (tracingEnabled) {
        throw new InvalidConfigException(
            "Define " + SAMPLE_RATE + " if " + TRACING_ENABLED + " is true in [" + url + "]");
      }
      sampleRate = 0;
    }

    TracingMode tracingMode =
        tracingEnabled == null || tracingEnabled ? TracingMode.ENABLED : TracingMode.DISABLED;
    return new TraceConfig(sampleRate, SampleRateSource.FILE, tracingMode.toFlags());
  }
}
