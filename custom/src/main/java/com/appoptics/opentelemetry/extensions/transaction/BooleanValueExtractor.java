package com.appoptics.opentelemetry.extensions.transaction;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Objects;

@Value
@EqualsAndHashCode(callSuper = false)
public class BooleanValueExtractor extends AttributeValueExtractor{
    BooleanValueExtractor(AttributeValueExtractor nextExtractor, String delimiter) {
        super(nextExtractor, delimiter);
    }

    @Override
    public StringBuilder extract(String attributeKey, Attributes attributes) {
        StringBuilder name = new StringBuilder();
        if (attributes.get(AttributeKey.booleanArrayKey(attributeKey)) != null) {
            Objects.requireNonNull(attributes.get(AttributeKey.booleanArrayKey(attributeKey)))
                    .forEach(innerAttr ->
                            name.append(innerAttr)
                                    .append(delimiter)
                    );
            return name;

        } else if (attributes.get(AttributeKey.booleanKey(attributeKey)) != null) {
            Boolean value = attributes.get(AttributeKey.booleanKey(attributeKey));
            if (value != null) {
                name.append(value)
                        .append(delimiter);
            }
            return name;
        }

        return nextExtractor.extract(attributeKey, attributes);
    }
}
