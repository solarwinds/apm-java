package com.appoptics.opentelemetry.extensions.initialize.config;

import com.tracelytics.ext.json.JSONArray;
import com.tracelytics.ext.json.JSONException;
import com.tracelytics.ext.json.JSONObject;
import com.tracelytics.joboe.SampleRateSource;
import com.tracelytics.joboe.TraceConfig;
import com.tracelytics.joboe.TracingMode;
import com.tracelytics.joboe.config.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Parses the json config value from `agent.transactionSettings` and produces a {@link TraceConfigs}
 * @author pluk
 *
 */
public class TransactionSettingsConfigParser implements ConfigParser<String, TraceConfigs> {
    private static final String TRACING_KEY = "tracing";
    private static final String REGEX_KEY = "regex";
    private static final String EXTENSIONS_KEY = "com/appoptics/opentelemetry/extensions";
    @Deprecated
    private static final String TYPE_KEY = "type";
    private static final List<String> validTypes = Arrays.asList("url");

    private static final List<String> KEYS = Arrays.asList(TRACING_KEY, EXTENSIONS_KEY, REGEX_KEY, TYPE_KEY);

    public static final TransactionSettingsConfigParser INSTANCE = new TransactionSettingsConfigParser();

    private TransactionSettingsConfigParser() {
    }


    @Override
    public TraceConfigs convert(String transactionSettingValue) throws InvalidConfigException {
        try {
            JSONArray array = new JSONArray(transactionSettingValue);
            Map<ResourceMatcher, TraceConfig> result = new LinkedHashMap<ResourceMatcher, TraceConfig>();
            for (int i = 0 ; i < array.length(); i++) {
                JSONObject entry = array.getJSONObject(i);

                ResourceMatcher matcher = parseMatcher(entry);
                TraceConfig traceConfig = parseTraceConfig(entry);

                result.put(matcher, traceConfig);
            }

            return new TraceConfigs(result);
        } catch (JSONException e) {
            throw new InvalidConfigException("Failed to parse \"" + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey() + "\". Error message is [" + e.getMessage() + "]", e);
        }
    }

    private ResourceMatcher parseMatcher(JSONObject transactionSettingEntry) throws InvalidConfigException, JSONException {
        checkKeys(transactionSettingEntry.keySet());

//        if (!transactionSettingEntry.has(TYPE_KEY)) {
//            throw new InvalidConfigException("Missing property \"" + TYPE_KEY + "\" in \"" + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey() + "\" entry " + transactionSettingEntry.toString());
//        }

//        String type = transactionSettingEntry.getString(TYPE_KEY);
//        if (!validTypes.contains(type)) {
//            throw new InvalidConfigException("Property \"" + TYPE_KEY + "\" with value " + type + " in \"" + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey() + "\" entry " + transactionSettingEntry.toString() + " is invalid. Valid types : " + validTypes);
//        }

        if (transactionSettingEntry.has(REGEX_KEY) && transactionSettingEntry.has(EXTENSIONS_KEY)) {
            throw new InvalidConfigException("Multiple matchers found for \"" + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey() + "\" entry " + transactionSettingEntry.toString());
        } else if (transactionSettingEntry.has(REGEX_KEY)) {
            String regexString = transactionSettingEntry.getString(REGEX_KEY);
            Pattern pattern;
            try {
                pattern = Pattern.compile(regexString, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                throw new InvalidConfigException("Failed to compile pattern " + regexString + " defined in \"" + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey() + "." + REGEX_KEY + "\", error message [" + e.getMessage() + "].", e);
            }
            return new StringPatternMatcher(pattern);
        } else if (transactionSettingEntry.has(EXTENSIONS_KEY)) {
            JSONArray resourceExtensionsJson = transactionSettingEntry.getJSONArray(EXTENSIONS_KEY);
            Set<String> resourceExtensions = new HashSet<String>();
            for (int j = 0 ; j < resourceExtensionsJson.length(); j ++) {
                resourceExtensions.add(resourceExtensionsJson.getString(j));
            }
            return new ResourceExtensionsMatcher(resourceExtensions);
        } else {
            throw new InvalidConfigException("Cannot find proper matcher for \"" + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey() + "\" entry " + transactionSettingEntry.toString() + ". Neither " + REGEX_KEY + " nor " + EXTENSIONS_KEY + " was defined");
        }
    }

    private void checkKeys(Set<?> jsonKeys) throws InvalidConfigException {
        Set<Object> keys = new HashSet<Object>(jsonKeys);
        keys.removeAll(KEYS);

        if (!keys.isEmpty()) {
            throw new InvalidConfigException("Failed to parse \"" +  ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey() + "\". Unknown key(s) " + keys + " found");
        }

    }


    private TraceConfig parseTraceConfig(JSONObject transactionSettingEntry) throws InvalidConfigException, JSONException {
        Set<?> keys = transactionSettingEntry.keySet();
        TracingMode tracingMode = null;

        if (keys.contains(TRACING_KEY)) {
            String tracingModeString = transactionSettingEntry.getString(TRACING_KEY);
            tracingMode = TracingMode.fromString(tracingModeString);
            if (tracingMode == null) {
                throw new InvalidConfigException("Invalid \"" + TRACING_KEY + "\" value [" + tracingModeString + "], must either be " + TracingMode.ENABLED.getStringValue() + " or " + TracingMode.DISABLED.getStringValue());
            }
            if (tracingMode == TracingMode.ALWAYS || tracingMode == TracingMode.ENABLED) {
                return new TraceConfig(null, SampleRateSource.FILE, tracingMode.toFlags()); //undefined sample rate if trace mode is enabled
            } else {
                return new TraceConfig(0, SampleRateSource.FILE, tracingMode.toFlags());
            }
        } else {
            throw new InvalidConfigException("Need to define \"" + TRACING_KEY + "\" for each entry in \"" + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey() + "\"");
        }
    }

}