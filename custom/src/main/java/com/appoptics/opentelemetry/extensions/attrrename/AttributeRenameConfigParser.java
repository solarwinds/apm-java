package com.appoptics.opentelemetry.extensions.attrrename;

import com.tracelytics.joboe.config.ConfigParser;
import com.tracelytics.joboe.config.InvalidConfigException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AttributeRenameConfigParser implements ConfigParser<String, Map<String, String>> {

    @Override
    public Map<String, String> convert(String attrMap) throws InvalidConfigException {
        String[] kvs = attrMap.split(",");
        Map<String, String> parsed = new HashMap<>(), dupCheck = new HashMap<>();

        for (String kv : kvs) {
            String[] pair = kv.split("=");
            if (pair.length != 2) {
                throw new InvalidConfigException(String.format("Invalid key-value pair (%s)", kv));
            }

            if (parsed.containsKey(pair[0])) {
                throw new InvalidConfigException(String.format("Duplicate key: (%s)", pair[0]));
            }

            if (dupCheck.containsKey(pair[1])) {
                throw new InvalidConfigException(String.format("Name collision. (%s) is already mapped with key (%s)",
                        pair[1], dupCheck.get(pair[1])));
            }

            String key = pair[0].trim().toLowerCase(Locale.ROOT), value = pair[1].trim().toLowerCase(Locale.ROOT);
            parsed.put(key, value);
            dupCheck.put(value, key);
        }

        return parsed;
    }
}
