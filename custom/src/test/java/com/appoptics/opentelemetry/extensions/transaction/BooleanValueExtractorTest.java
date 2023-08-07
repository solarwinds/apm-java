package com.appoptics.opentelemetry.extensions.transaction;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BooleanValueExtractorTest {

    private BooleanValueExtractor tested;

    @Mock
    private AttributeValueExtractor attributeValueExtractorMock;

    @BeforeEach
    void setup() {
        tested = new BooleanValueExtractor(attributeValueExtractorMock, "");
    }

    @Test
    void returnStringBuilderValueGivenBooleanListValue() {
        String attributeKey = "key-name";
        Attributes attributes = Attributes.of(AttributeKey.booleanArrayKey(attributeKey), Arrays.asList(false, true));

        StringBuilder actual = tested.extract(attributeKey, attributes);
        assertEquals("falsetrue", actual.toString());
    }

    @Test
    void returnStringBuilderValueGivenBooleanValue() {
        String attributeKey = "key-name";
        Attributes attributes = Attributes.of(AttributeKey.booleanKey(attributeKey), false);

        StringBuilder actual = tested.extract(attributeKey, attributes);
        assertEquals("false", actual.toString());
    }

    @Test
    void verifyThatNextExtractorIsDelegateWhenAttributeKeyIsNotBooleanKey() {
        String attributeKey = "key-name";
        Attributes attributes = Attributes.of(AttributeKey.stringKey(attributeKey), "false");

        tested.extract(attributeKey, attributes);
        verify(attributeValueExtractorMock).extract(any(), any());

    }
}