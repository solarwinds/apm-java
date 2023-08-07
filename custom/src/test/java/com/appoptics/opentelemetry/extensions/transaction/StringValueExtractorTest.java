package com.appoptics.opentelemetry.extensions.transaction;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
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
class StringValueExtractorTest {

    private StringValueExtractor tested;

    @Mock
    private AttributeValueExtractor attributeValueExtractorMock;

    @BeforeEach
    void setup() {
        tested = new StringValueExtractor(attributeValueExtractorMock, "");
    }

    @Test
    void returnStringBuilderValueGivenStringListValue() {
        String attributeKey = "key-name";
        Attributes attributes = Attributes.of(AttributeKey.stringArrayKey(attributeKey), Arrays.asList("Hell", "o"));

        StringBuilder actual = tested.extract(attributeKey, attributes);
        assertEquals("Hello", actual.toString());
    }

    @Test
    void returnStringBuilderValueGivenStringValue() {
        String attributeKey = "key-name";
        Attributes attributes = Attributes.of(AttributeKey.stringKey(attributeKey), "Hello");

        StringBuilder actual = tested.extract(attributeKey, attributes);
        assertEquals("Hello", actual.toString());
    }

    @Test
    void verifyThatNextExtractorIsDelegateWhenAttributeKeyIsNotStringKey() {
        String attributeKey = "key-name";
        Attributes attributes = Attributes.of(AttributeKey.longKey(attributeKey), 90L);

        tested.extract(attributeKey, attributes);
        verify(attributeValueExtractorMock).extract(any(), any());
    }

}