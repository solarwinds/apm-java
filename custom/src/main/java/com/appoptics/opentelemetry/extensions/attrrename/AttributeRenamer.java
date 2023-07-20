package com.appoptics.opentelemetry.extensions.attrrename;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;

import java.util.Map;

public final class AttributeRenamer {
    private final Map<String, String> attrMap;

    private static AttributeRenamer INSTANCE;

    private AttributeRenamer(Map<String, String> attrMap) {
        this.attrMap = attrMap;
    }

    public static AttributeRenamer initialize(Map<String, String> attrMap) {
        if (INSTANCE == null) {
            INSTANCE = new AttributeRenamer(attrMap);
        }

        return INSTANCE;
    }

    public static AttributeRenamer getInstance() {
        return INSTANCE;
    }

    public String rename(String attr) {
        return attrMap.getOrDefault(attr, attr);
    }

    public Attributes rename(Attributes attributes) {
        AttributesBuilder builder = attributes.toBuilder();
        attrMap.forEach(((oldAttr, newAttr) -> {
            AttributeKey<String> stringAttributeKey = AttributeKey.stringKey(oldAttr);
            String value = attributes.get(stringAttributeKey);

            if (value != null) {
                builder.put(AttributeKey.stringKey(newAttr), value);
                builder.remove(stringAttributeKey);
            }
        }));
        return builder.build();
    }
}
