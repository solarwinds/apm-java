package com.appoptics.opentelemetry.extensions.transaction;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Objects;

@Value
@EqualsAndHashCode(callSuper = false)
public class StringValueExtractor extends AttributeValueExtractor{
    StringValueExtractor(AttributeValueExtractor nextExtractor, String delimiter) {
        super(nextExtractor, delimiter);
    }

    @Override
    public StringBuilder extract(String attributeKey, Attributes attributes) {
        StringBuilder name = new StringBuilder();
        if (attributes.get(AttributeKey.stringArrayKey(attributeKey)) != null) {
            Objects.requireNonNull(attributes.get(AttributeKey.stringArrayKey(attributeKey)))
                    .forEach(innerAttr ->
                            name.append(innerAttr)
                                    .append(delimiter)
                    );

            return name;
        } else if (attributes.get(AttributeKey.stringKey(attributeKey)) != null) {
            String value = attributes.get(AttributeKey.stringKey(attributeKey));
            if (value != null) {
                name.append(value)
                        .append(delimiter);
            }
            return name;
        }

        return nextExtractor.extract(attributeKey, attributes);
    }
}
