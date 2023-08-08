package com.appoptics.opentelemetry.extensions.transaction;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import org.checkerframework.common.value.qual.DoubleVal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DoubleValueExtractorTest {

    private DoubleValueExtractor tested;

    @Mock
    private AttributeValueExtractor attributeValueExtractorMock;

    @BeforeEach
    void setup() {
        tested = new DoubleValueExtractor(attributeValueExtractorMock, "");
    }

    @Test
    void returnStringBuilderValueGivenDoubleListValue() {
        String attributeKey = "key-name";
        Attributes attributes = Attributes.of(AttributeKey.doubleArrayKey(attributeKey), Arrays.asList(23.0, 0.0));

        StringBuilder actual = tested.extract(attributeKey, attributes);
        assertEquals("23.00.0", actual.toString());
    }

    @Test
    void returnStringBuilderValueGivenDoubleValue() {
        String attributeKey = "key-name";
        Attributes attributes = Attributes.of(AttributeKey.doubleKey(attributeKey), 233.98);

        StringBuilder actual = tested.extract(attributeKey, attributes);
        assertEquals("233.98", actual.toString());
    }

    @Test
    void verifyThatNextExtractorIsDelegateWhenAttributeKeyIsNotDoubleKey() {
        String attributeKey = "key-name";
        Attributes attributes = Attributes.of(AttributeKey.stringKey(attributeKey), "false");

        tested.extract(attributeKey, attributes);
        verify(attributeValueExtractorMock).extract(any(), any());

    }
}