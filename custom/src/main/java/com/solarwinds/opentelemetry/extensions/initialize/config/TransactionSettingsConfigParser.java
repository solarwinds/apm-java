package com.solarwinds.opentelemetry.extensions.initialize.config;

import com.solarwinds.joboe.core.SampleRateSource;
import com.solarwinds.joboe.core.TraceConfig;
import com.solarwinds.joboe.core.TracingMode;
import com.solarwinds.joboe.core.config.ConfigParser;
import com.solarwinds.joboe.core.config.ConfigProperty;
import com.solarwinds.joboe.core.config.InvalidConfigException;
import com.solarwinds.joboe.core.config.ResourceMatcher;
import com.solarwinds.joboe.core.config.TraceConfigs;
import com.solarwinds.joboe.shaded.org.json.JSONArray;
import com.solarwinds.joboe.shaded.org.json.JSONException;
import com.solarwinds.joboe.shaded.org.json.JSONObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Parses the json config value from `agent.transactionSettings` and produces a {@link TraceConfigs}
 *
 * @author pluk
 */
public class TransactionSettingsConfigParser implements ConfigParser<String, TraceConfigs> {
  private static final String TRACING_KEY = "tracing";
  private static final String REGEX_KEY = "regex";
  private static final String EXTENSIONS_KEY = "com/solarwinds/opentelemetry/extensions";

  private static final List<String> KEYS = Arrays.asList(TRACING_KEY, EXTENSIONS_KEY, REGEX_KEY);

  public static final TransactionSettingsConfigParser INSTANCE =
      new TransactionSettingsConfigParser();

  private TransactionSettingsConfigParser() {}

  @Override
  public TraceConfigs convert(String transactionSettingValue) throws InvalidConfigException {
    try {
      JSONArray array = new JSONArray(transactionSettingValue);
      Map<ResourceMatcher, TraceConfig> result = new LinkedHashMap<ResourceMatcher, TraceConfig>();
      for (int i = 0; i < array.length(); i++) {
        JSONObject entry = array.getJSONObject(i);

        ResourceMatcher matcher = parseMatcher(entry);
        TraceConfig traceConfig = parseTraceConfig(entry);

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

  private ResourceMatcher parseMatcher(JSONObject transactionSettingEntry)
      throws InvalidConfigException, JSONException {
    checkKeys(transactionSettingEntry.keySet());

    if (transactionSettingEntry.has(REGEX_KEY) && transactionSettingEntry.has(EXTENSIONS_KEY)) {
      throw new InvalidConfigException(
          "Multiple matchers found for \""
              + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey()
              + "\" entry "
              + transactionSettingEntry.toString());
    } else if (transactionSettingEntry.has(REGEX_KEY)) {
      String regexString = transactionSettingEntry.getString(REGEX_KEY);
      Pattern pattern;
      try {
        pattern = Pattern.compile(regexString, Pattern.CASE_INSENSITIVE);
      } catch (PatternSyntaxException e) {
        throw new InvalidConfigException(
            "Failed to compile pattern "
                + regexString
                + " defined in \""
                + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey()
                + "."
                + REGEX_KEY
                + "\", error message ["
                + e.getMessage()
                + "].",
            e);
      }
      return new StringPatternMatcher(pattern);
    } else if (transactionSettingEntry.has(EXTENSIONS_KEY)) {
      JSONArray resourceExtensionsJson = transactionSettingEntry.getJSONArray(EXTENSIONS_KEY);
      Set<String> resourceExtensions = new HashSet<String>();
      for (int j = 0; j < resourceExtensionsJson.length(); j++) {
        resourceExtensions.add(resourceExtensionsJson.getString(j));
      }
      return new ResourceExtensionsMatcher(resourceExtensions);
    } else {
      throw new InvalidConfigException(
          "Cannot find proper matcher for \""
              + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey()
              + "\" entry "
              + transactionSettingEntry.toString()
              + ". Neither "
              + REGEX_KEY
              + " nor "
              + EXTENSIONS_KEY
              + " was defined");
    }
  }

  private void checkKeys(Set<?> jsonKeys) throws InvalidConfigException {
    Set<Object> keys = new HashSet<Object>(jsonKeys);
    keys.removeAll(KEYS);

    if (!keys.isEmpty()) {
      throw new InvalidConfigException(
          "Failed to parse \""
              + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey()
              + "\". Unknown key(s) "
              + keys
              + " found");
    }
  }

  private TraceConfig parseTraceConfig(JSONObject transactionSettingEntry)
      throws InvalidConfigException, JSONException {
    Set<?> keys = transactionSettingEntry.keySet();
    TracingMode tracingMode;

    if (keys.contains(TRACING_KEY)) {
      String tracingModeString = transactionSettingEntry.getString(TRACING_KEY);
      tracingMode = TracingMode.fromString(tracingModeString);
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
