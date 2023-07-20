package com.appoptics.opentelemetry.extensions.transaction;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.Objects;

@Value
@EqualsAndHashCode(callSuper = false)
public class SpanAttributeNamingScheme extends NamingScheme {
    String delimiter;
    List<String> attributes;

    public SpanAttributeNamingScheme(NamingScheme next, String delimiter, List<String> attributes) {
        super(next);
        this.delimiter = delimiter;
        this.attributes = attributes;
    }

    @Override
    public String createName(Attributes attributes) {
        StringBuilder name = new StringBuilder();
        this.attributes.forEach(attr -> {
            if (attributes.get(AttributeKey.stringArrayKey(attr)) != null) {
                Objects.requireNonNull(attributes.get(AttributeKey.stringArrayKey(attr)))
                        .forEach(innerAttr ->
                                name.append(innerAttr)
                                        .append(delimiter)
                        );

            } else if (attributes.get(AttributeKey.stringKey(attr)) != null) {
                String value = attributes.get(AttributeKey.stringKey(attr));
                if (value != null) {
                    name.append(value)
                            .append(delimiter);
                }
            }
        });

        if (name.length() > 0) {
            return name.substring(0, name.length() - delimiter.length());
        }

        return next.createName(attributes);
    }
}




