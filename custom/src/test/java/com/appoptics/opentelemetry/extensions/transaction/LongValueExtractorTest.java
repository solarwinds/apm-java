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
class LongValueExtractorTest {

    private LongValueExtractor tested;

    @Mock
    private AttributeValueExtractor attributeValueExtractorMock;

    @BeforeEach
    void setup() {
        tested = new LongValueExtractor(attributeValueExtractorMock, "");
    }

    @Test
    void returnStringBuilderValueGivenLongListValue() {
        String attributeKey = "key-name";
        Attributes attributes = Attributes.of(AttributeKey.longArrayKey(attributeKey), Arrays.asList(23L, 0L));

        StringBuilder actual = tested.extract(attributeKey, attributes);
        assertEquals("230", actual.toString());
    }

    @Test
    void returnStringBuilderValueGivenLongValue() {
        String attributeKey = "key-name";
        Attributes attributes = Attributes.of(AttributeKey.longKey(attributeKey), 230L);

        StringBuilder actual = tested.extract(attributeKey, attributes);
        assertEquals("230", actual.toString());
    }

    @Test
    void verifyThatNextExtractorIsDelegateWhenAttributeKeyIsNotLongKey() {
        String attributeKey = "key-name";
        Attributes attributes = Attributes.of(AttributeKey.stringKey(attributeKey), "false");

        tested.extract(attributeKey, attributes);
        verify(attributeValueExtractorMock).extract(any(), any());

    }

}