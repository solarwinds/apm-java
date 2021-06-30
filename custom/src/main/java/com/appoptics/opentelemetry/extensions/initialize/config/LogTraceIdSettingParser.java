package com.appoptics.opentelemetry.extensions.initialize.config;

import com.tracelytics.ext.json.JSONException;
import com.tracelytics.ext.json.JSONObject;
import com.tracelytics.joboe.config.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LogTraceIdSettingParser implements ConfigParser<String, LogTraceIdSetting> {
    public static final LogTraceIdSettingParser INSTANCE = new LogTraceIdSettingParser();
    private static final String MDC_KEY = "mdc";
    private static final String AUTO_INSERT_KEY = "autoInsert";

    private static final Map<String, LogTraceIdScope> labelToScopes = new HashMap<String, LogTraceIdScope>();

    private static final String ENABLED_LABEL = "enabled";
    private static final String DISABLED_LABEL = "disabled";
    private static final String SAMPLED_ONLY_LABEL = "sampledOnly";
    
    
    static {
        labelToScopes.put(ENABLED_LABEL, LogTraceIdScope.ENABLED);
        labelToScopes.put(DISABLED_LABEL, LogTraceIdScope.DISABLED);
        labelToScopes.put(SAMPLED_ONLY_LABEL, LogTraceIdScope.SAMPLED_ONLY);
    }

    private LogTraceIdSettingParser() {
    }
    

    @Override
    public LogTraceIdSetting convert(String logTraceIdSettingString) throws InvalidConfigException {
        try {
            JSONObject jsonObject = new JSONObject(logTraceIdSettingString);

            String autoInsertScopeString = jsonObject.optString(AUTO_INSERT_KEY);
            String mdcScopeString = jsonObject.optString(MDC_KEY);
            
            Set<Object> unknownKeys = new HashSet<Object>(jsonObject.keySet());
            unknownKeys.remove(MDC_KEY);
            unknownKeys.remove(AUTO_INSERT_KEY);
            
            if (!unknownKeys.isEmpty()) {
                throw new InvalidConfigException("Invalid settings for " + ConfigProperty.AGENT_LOGGING_TRACE_ID.getConfigFileKey() + ". Found unknown key(s): " + unknownKeys);
            }
            
            if ("".equals(autoInsertScopeString) && "".equals(mdcScopeString)) {
                throw new InvalidConfigException("Invalid empty settings for " + ConfigProperty.AGENT_LOGGING_TRACE_ID.getConfigFileKey());
            }

            LogTraceIdScope autoInsertScope;
            if ("".equals(autoInsertScopeString)) {
                autoInsertScope = LogTraceIdScope.DISABLED;
            } else {
                autoInsertScope = labelToScopes.get(autoInsertScopeString);
                if (autoInsertScope == null) {
                    throw new InvalidConfigException("Invalid value for " + AUTO_INSERT_KEY + " in " + ConfigProperty.AGENT_LOGGING_TRACE_ID.getConfigFileKey() + " found value [" + autoInsertScopeString + "]. It should be one of the values in " + labelToScopes.keySet());
                }
            }

            LogTraceIdScope mdcScope;
            if ("".equals(mdcScopeString)) {
                mdcScope = LogTraceIdScope.DISABLED;
            } else {
                mdcScope = labelToScopes.get(mdcScopeString);
                if (mdcScope == null) {
                    throw new InvalidConfigException("Invalid value for " + MDC_KEY + " in " + ConfigProperty.AGENT_LOGGING_TRACE_ID.getConfigFileKey() + " found value [" + mdcScopeString + "]. It should be one of the values in " + labelToScopes.keySet());
                }
            }
            

            return new LogTraceIdSetting(autoInsertScope, mdcScope);
        } catch (JSONException e) {
            throw new InvalidConfigException("Failed to parse \"" + ConfigProperty.AGENT_LOGGING_TRACE_ID.getConfigFileKey() + "\". Error message is [" + e.getMessage() + "]", e);
        }
    }
}
