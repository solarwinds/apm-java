package com.appoptics.opentelemetry.extensions.transaction;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Objects;

@Value
@EqualsAndHashCode(callSuper = false)
public class DoubleValueExtractor extends AttributeValueExtractor{
    DoubleValueExtractor(AttributeValueExtractor nextExtractor, String delimiter) {
        super(nextExtractor, delimiter);
    }

    @Override
    public StringBuilder extract(String attributeKey, Attributes attributes) {
        StringBuilder name = new StringBuilder();
        if (attributes.get(AttributeKey.doubleArrayKey(attributeKey)) != null) {
            Objects.requireNonNull(attributes.get(AttributeKey.doubleArrayKey(attributeKey)))
                    .forEach(innerAttr ->
                            name.append(innerAttr)
                                    .append(delimiter)
                    );
            return name;

        } else if (attributes.get(AttributeKey.doubleKey(attributeKey)) != null) {
            Double value = attributes.get(AttributeKey.doubleKey(attributeKey));
            if (value != null) {
                name.append(value)
                        .append(delimiter);
            }
            return name;
        }

        return nextExtractor.extract(attributeKey, attributes);
    }
}
