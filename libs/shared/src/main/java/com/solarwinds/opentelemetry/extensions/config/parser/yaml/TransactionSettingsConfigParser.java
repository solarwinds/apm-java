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
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.sampling.ResourceMatcher;
import com.solarwinds.joboe.sampling.SampleRateSource;
import com.solarwinds.joboe.sampling.TraceConfig;
import com.solarwinds.joboe.sampling.TraceConfigs;
import com.solarwinds.joboe.sampling.TracingMode;
import com.solarwinds.opentelemetry.extensions.config.parser.json.ResourceExtensionsMatcher;
import com.solarwinds.opentelemetry.extensions.config.parser.json.StringPatternMatcher;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.json.JSONException;

@SuppressWarnings("rawtypes")
@AutoService(ConfigParser.class)
public class TransactionSettingsConfigParser
    implements ConfigParser<DeclarativeConfigProperties, TraceConfigs> {
  private static final String TRACING_KEY = "tracing";
  private static final String REGEX_KEY = "regex";
  private static final String EXTENSIONS_KEY = "com/solarwinds/opentelemetry/extensions";

  private static final List<String> KEYS = Arrays.asList(TRACING_KEY, EXTENSIONS_KEY, REGEX_KEY);
  private static final String CONFIG_KEY = "agent.transactionSettings";

  @Override
  public TraceConfigs convert(DeclarativeConfigProperties declarativeConfigProperties)
      throws InvalidConfigException {
    try {
      List<DeclarativeConfigProperties> transactionSettings =
          declarativeConfigProperties.getStructuredList(CONFIG_KEY, Collections.emptyList());

      Map<ResourceMatcher, TraceConfig> result = new LinkedHashMap<ResourceMatcher, TraceConfig>();
      for (DeclarativeConfigProperties transactionSetting : transactionSettings) {
        ResourceMatcher matcher = parseMatcher(transactionSetting);
        TraceConfig traceConfig = parseTraceConfig(transactionSetting);

        result.put(matcher, traceConfig);
      }

      return new TraceConfigs(result);
    } catch (JSONException e) {
      throw new InvalidConfigException(
          "Failed to parse \""
              + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey()
              + "\". Error message is ["
              + e.getMessage()
              + "]",
          e);
    }
  }

  @Override
  public String configKey() {
    return CONFIG_KEY;
  }

  private ResourceMatcher parseMatcher(DeclarativeConfigProperties declarativeConfigProperties)
      throws InvalidConfigException, JSONException {
    checkKeys(declarativeConfigProperties.getPropertyKeys());
    String regex = declarativeConfigProperties.getString(REGEX_KEY);
    List<String> extensions =
        declarativeConfigProperties.getScalarList(EXTENSIONS_KEY, String.class);

    if (regex != null && extensions != null) {
      throw new InvalidConfigException(
          "Multiple matchers found for \""
              + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey()
              + "\" entry "
              + declarativeConfigProperties);
    } else if (regex != null) {
      try {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return new StringPatternMatcher(pattern);
      } catch (PatternSyntaxException e) {
        throw new InvalidConfigException(
            "Failed to compile pattern "
                + regex
                + " defined in \""
                + configKey()
                + "."
                + REGEX_KEY
                + "\", error message ["
                + e.getMessage()
                + "].",
            e);
      }

    } else if (extensions != null) {
      Set<String> resourceExtensions = new HashSet<String>(extensions);
      return new ResourceExtensionsMatcher(resourceExtensions);
    } else {
      throw new InvalidConfigException(
          "Cannot find proper matcher for \""
              + configKey()
              + "\" entry "
              + declarativeConfigProperties
              + ". Neither "
              + REGEX_KEY
              + " nor "
              + EXTENSIONS_KEY
              + " was defined");
    }
  }

  private void checkKeys(Set<?> jsonKeys) throws InvalidConfigException {
    Set<Object> keys = new HashSet<Object>(jsonKeys);
    KEYS.forEach(keys::remove);

    if (!keys.isEmpty()) {
      throw new InvalidConfigException(
          "Failed to parse \""
              + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey()
              + "\". Unknown key(s) "
              + keys
              + " found");
    }
  }

  private TraceConfig parseTraceConfig(DeclarativeConfigProperties declarativeConfigProperties)
      throws InvalidConfigException, JSONException {
    String tracingModeString = declarativeConfigProperties.getString(TRACING_KEY);

    if (tracingModeString != null) {
      TracingMode tracingMode = TracingMode.fromString(tracingModeString);
      if (tracingMode == null) {
        throw new InvalidConfigException(
            "Invalid \""
                + TRACING_KEY
                + "\" value ["
                + tracingModeString
                + "], must either be "
                + TracingMode.ENABLED.getStringValue()
                + " or "
                + TracingMode.DISABLED.getStringValue());
      }
      if (tracingMode == TracingMode.ALWAYS || tracingMode == TracingMode.ENABLED) {
        return new TraceConfig(
            null,
            SampleRateSource.FILE,
            tracingMode.toFlags()); // undefined sample rate if trace mode is enabled
      } else {
        return new TraceConfig(0, SampleRateSource.FILE, tracingMode.toFlags());
      }
    } else {
      throw new InvalidConfigException(
          "Need to define \""
              + TRACING_KEY
              + "\" for each entry in \""
              + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey()
              + "\"");
    }
  }
}
