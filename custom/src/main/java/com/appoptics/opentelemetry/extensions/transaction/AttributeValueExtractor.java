package com.appoptics.opentelemetry.extensions.transaction;

import io.opentelemetry.api.common.Attributes;

public abstract class AttributeValueExtractor {

    protected final AttributeValueExtractor nextExtractor;
    protected final String delimiter;
    protected AttributeValueExtractor(AttributeValueExtractor nextExtractor, String delimiter) {
        this.nextExtractor = nextExtractor;
        this.delimiter = delimiter;
    }

    public abstract StringBuilder extract(String attributeKey, Attributes attributes);
}
