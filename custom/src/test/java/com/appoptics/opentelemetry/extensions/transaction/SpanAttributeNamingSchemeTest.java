package com.appoptics.opentelemetry.extensions.transaction;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpanAttributeNamingSchemeTest {
    private SpanAttributeNamingScheme tested;

    @Mock
    private SpanData spanDataMock;

    @Mock
    private NamingScheme namingSchemeMock;

    @BeforeEach
    void setup() {
        tested = new SpanAttributeNamingScheme(namingSchemeMock, "-", Arrays.asList("http.method", "Handler"));
    }

    @Test
    void verifyThatNameIsReturnedWhenOneAttributeExists() {
        String name = tested.createName(Attributes.of(SemanticAttributes.HTTP_METHOD, "POST"));
        assertEquals("POST", name);
    }

    @Test
    void verifyThatNameIsReturnedWhenMoreThanOneAttributesExist() {
        Attributes attributes = Attributes.builder()
                .put(SemanticAttributes.HTTP_METHOD, "POST")
                .put(AttributeKey.stringKey("Handler"), "Controller.segfault")
                .build();

        String name = tested.createName(attributes);
        assertEquals("POST-Controller.segfault", name);
    }


    @Test
    void verifyThatNameIsReturnedWhenAttributeValueIsList() {
        Attributes attributes = Attributes.builder()
                .put(AttributeKey.stringArrayKey("Handler"), Arrays.asList("POST", "Controller.segfault"))
                .build();

        String name = tested.createName(attributes);
        assertEquals("POST-Controller.segfault", name);
    }

    @Test
    void verifyDelegationToNextWhenNoMatchingAttributeExist() {
        when(namingSchemeMock.createName(any())).thenReturn("Hello");
        String name = tested.createName(Attributes.empty());
        assertEquals("Hello", name);
    }
}