package com.appoptics.opentelemetry.extensions.transaction;

import io.opentelemetry.api.common.Attributes;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class SpanAttributeNamingScheme extends NamingScheme {
    String delimiter;
    List<String> attributes;
    AttributeValueExtractor attributeValueExtractor;

    public SpanAttributeNamingScheme(NamingScheme next, String delimiter, List<String> attributes) {
        super(next);
        this.delimiter = delimiter;
        this.attributes = attributes;

        attributeValueExtractor = new StringValueExtractor(new DoubleValueExtractor(new LongValueExtractor(new BooleanValueExtractor(
                new DefaultValueExtractor(null, null), delimiter
        ), delimiter), delimiter), delimiter);
    }

    @Override
    public String createName(Attributes attributes) {
        StringBuilder name = new StringBuilder();
        for (String attributeKey : this.attributes) {
            name.append(attributeValueExtractor.extract(attributeKey, attributes));
        }

        if (name.length() > 0) {
            return name.substring(0, name.length() - delimiter.length());
        }
        return next.createName(attributes);
    }
}




