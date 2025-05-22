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

package com.solarwinds.opentelemetry.extensions.config.parser.yaml;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.ConfigParser;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.sampling.ResourceMatcher;
import com.solarwinds.joboe.sampling.SampleRateSource;
import com.solarwinds.joboe.sampling.TraceConfig;
import com.solarwinds.joboe.sampling.TraceConfigs;
import com.solarwinds.joboe.sampling.TracingMode;
import com.solarwinds.opentelemetry.extensions.config.parser.json.StringPatternMatcher;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@AutoService(ConfigParser.class)
public class UrlSampleRateConfigParser
    implements ConfigParser<DeclarativeConfigProperties, TraceConfigs> {

  private static final String SAMPLE_RATE = "sampleRate";
  private static final String TRACING_MODE = "tracingMode";
  private static final String CONFIG_KEY = "agent.urlSampleRates";

  @Override
  public TraceConfigs convert(DeclarativeConfigProperties declarativeConfigProperties)
      throws InvalidConfigException {
    List<DeclarativeConfigProperties> urlSampleRates =
        declarativeConfigProperties.getStructuredList(CONFIG_KEY, Collections.emptyList());
    Map<ResourceMatcher, TraceConfig> result = new LinkedHashMap<>();
    for (DeclarativeConfigProperties urlSampleRate : urlSampleRates) {

      Set<String> propertyKeys = urlSampleRate.getPropertyKeys();
      if (propertyKeys.size() != 1) {
        throw new InvalidConfigException("Invalid url sample rate config");
      }

      Pattern pattern;
      String url = propertyKeys.iterator().next();
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
      TraceConfig traceConfig = extractConfig(urlConfig, declarativeConfigProperties);

      if (traceConfig != null) {
        result.put(new StringPatternMatcher(pattern), traceConfig);
      }
    }

    return new TraceConfigs(result);
  }

  @Override
  public String configKey() {
    return CONFIG_KEY;
  }

  private TraceConfig extractConfig(
      DeclarativeConfigProperties urlConfigEntry, DeclarativeConfigProperties urlConfig)
      throws InvalidConfigException {

    Integer sampleRate = urlConfigEntry.getInt(SAMPLE_RATE);
    String tracingModeString = urlConfigEntry.getString(TRACING_MODE);
    TracingMode tracingMode = null;

    if (tracingModeString != null) {
      tracingMode = TracingMode.fromString(tracingModeString);
      if (tracingMode == null) {
        throw new InvalidConfigException(
            "Invalid tracingMode value ["
                + tracingModeString
                + "], must be \"never\" if default is to be overridden; otherwise do not specify this property in ["
                + urlConfig
                + "]");
      }
    }

    if (sampleRate == null && tracingMode == null) {
      throw new InvalidConfigException(
          "Need to define either tracingMode, sampleRate or metricsEnabled, but found none in ["
              + urlConfig
              + "]");
    }

    if (sampleRate == null) {
      if (tracingMode == TracingMode.ALWAYS) {
        throw new InvalidConfigException(
            "Define sampleRate if tracingMode is \"always\" in [" + urlConfig + "]");
      } else {
        sampleRate = 0;
      }
    }

    if (tracingMode == null) {
      tracingMode = TracingMode.ALWAYS; // default to ALWAYS if not provided
    }

    return new TraceConfig(sampleRate, SampleRateSource.FILE, tracingMode.toFlags());
  }
}
