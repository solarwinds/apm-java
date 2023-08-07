package com.appoptics.opentelemetry.extensions.transaction;

import io.opentelemetry.api.common.Attributes;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class DefaultValueExtractor extends AttributeValueExtractor{
    DefaultValueExtractor(AttributeValueExtractor nextExtractor, String delimiter) {
        super(nextExtractor, delimiter);
    }

    @Override
    public StringBuilder extract(String attributeKey, Attributes attributes) {
        return new StringBuilder();
    }
}
