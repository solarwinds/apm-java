package com.appoptics.opentelemetry.extensions.initialize.config;

import com.tracelytics.ext.json.JSONArray;
import com.tracelytics.ext.json.JSONException;
import com.tracelytics.ext.json.JSONObject;
import com.tracelytics.joboe.SampleRateSource;
import com.tracelytics.joboe.TraceConfig;
import com.tracelytics.joboe.TracingMode;
import com.tracelytics.joboe.config.ConfigParser;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.joboe.config.ResourceMatcher;
import com.tracelytics.joboe.config.TraceConfigs;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Parses the json config value from `agent.urlSampleRates` and produces a {@link TraceConfigs}
 * @author pluk
 *
 */
public class UrlSampleRateConfigParser implements ConfigParser<String, TraceConfigs> {
    private static final String SAMPLE_RATE = "sampleRate";
    private static final String TRACING_MODE = "tracingMode";
    public static final UrlSampleRateConfigParser INSTANCE = new UrlSampleRateConfigParser();

    private UrlSampleRateConfigParser() {
    }

    private TraceConfig extractConfig(JSONObject urlConfigEntry, String urlConfigString) throws InvalidConfigException {
        Set<?> keys = urlConfigEntry.keySet();
        Integer sampleRate = null;
        TracingMode tracingMode = null;

        if (keys.contains(SAMPLE_RATE)) {
            try {
                sampleRate = (Integer) urlConfigEntry.get(SAMPLE_RATE);
            } catch (ClassCastException e) {
                throw new InvalidConfigException("Incompatible param type for sampleRate, sampleRate should be [Integer] in [" + urlConfigString + "]", e);
            } catch (JSONException e) {
                throw new InvalidConfigException("Json exception during lookup of sampleRate, error message [" + e.getMessage() + "] in [" + urlConfigString + "]", e);
            }
        }

        if (keys.contains(TRACING_MODE)) {
            try {
                String tracingModeString = (String) urlConfigEntry.get(TRACING_MODE);
                tracingMode = TracingMode.fromString(tracingModeString);
                if (tracingMode == null) {
                    throw new InvalidConfigException("Invalid tracingMode value [" + tracingModeString + "], must be \"never\" if default is to be overridden; otherwise do not specify this property in [" + urlConfigString + "]");
                }
            } catch (ClassCastException e) {
                throw new InvalidConfigException("Incompatible param type for tracingMode, sampleRate should be [String] of value \"never\" if default is to be overridden; otherwise do not specify this property in [" + urlConfigString + "]", e);
            } catch (JSONException e) {
                throw new InvalidConfigException("Json exception during lookup of tracingMode, error message [" + e.getMessage() + "] in [" + urlConfigString + "]", e);
            }
        }


        if (sampleRate == null && tracingMode == null) {
            throw new InvalidConfigException("Need to define either tracingMode, sampleRate or metricsEnabled, but found none in [" + urlConfigString + "]");
        }


        if (sampleRate == null) {
            if (tracingMode == TracingMode.ALWAYS) {
                throw new InvalidConfigException("Define sampleRate if tracingMode is \"always\" in [" + urlConfigString + "]");
            } else {
                sampleRate = 0;
            }
        }

        if (tracingMode == null) {
            tracingMode = TracingMode.ALWAYS; //default to ALWAYS if not provided
        }

        return new TraceConfig(sampleRate, SampleRateSource.FILE, tracingMode.toFlags());
    }

    @Override
    public TraceConfigs convert(String urlSampleRatesString) throws InvalidConfigException {
        try {
            JSONArray array = new JSONArray(urlSampleRatesString);
            Map<ResourceMatcher, TraceConfig> result = new LinkedHashMap<ResourceMatcher, TraceConfig>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject urlRateEntry = array.getJSONObject(i);
                String objectName = (String) urlRateEntry.keys().next();

                if (objectName == null) {
                    throw new InvalidConfigException("Invalid url sample rate note found, index [" + i + "]");
                }

                Pattern pattern;
                try {
                    pattern = Pattern.compile(objectName, Pattern.CASE_INSENSITIVE);
                } catch (PatternSyntaxException e) {
                    throw new InvalidConfigException("Failed to compile the url sample rate of url pattern [" + objectName + "], error message [" + e.getMessage() + "].", e);
                }

                Object attributeObj = urlRateEntry.get(objectName);
                if (attributeObj instanceof JSONObject) {
                    TraceConfig traceConfig = extractConfig((JSONObject) attributeObj, urlSampleRatesString);

                    if (traceConfig != null) {
                        result.put(new StringPatternMatcher(pattern), traceConfig);
                    }
                } else {
                    throw new InvalidConfigException("Unexpected object for url sample rate item, expected JSONObject but found [" + attributeObj + "]");
                }
            }

            return new TraceConfigs(result);
        } catch (JSONException e) {
            throw new InvalidConfigException("Failed to parse the url sample rate string of [" + urlSampleRatesString + "]. Error message is [" + e.getMessage() + "]", e);
        }
    }
}
