package com.appoptics.opentelemetry.extensions.transaction;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultValueExtractorTest {

    @InjectMocks
    private DefaultValueExtractor tested;

    @Mock
    private AttributeValueExtractor attributeValueExtractorMock;

    @Test
    void returnEmptyStringBuilderGivenAttributes() {
        String attributeKey = "key-name";
        Attributes attributes = Attributes.of(AttributeKey.booleanArrayKey(attributeKey), Arrays.asList(false, true));

        StringBuilder actual = tested.extract(attributeKey, attributes);
        assertEquals("", actual.toString());
    }

    @Test
    void verifyThatNextExtractorIsNotDelegated() {
        String attributeKey = "key-name";
        Attributes attributes = Attributes.of(AttributeKey.stringKey(attributeKey), "false");

        tested.extract(attributeKey, attributes);
        verify(attributeValueExtractorMock, never()).extract(any(), any());

    }
}