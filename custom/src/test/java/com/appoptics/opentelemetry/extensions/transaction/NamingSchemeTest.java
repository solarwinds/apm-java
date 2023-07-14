package com.appoptics.opentelemetry.extensions.transaction;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NamingSchemeTest {

    @Test
    void verifyThatSpanAttributeNamingSchemeIsParsedCorrectly() {
        List<TransactionNamingScheme> schemes = Collections.singletonList(
                new TransactionNamingScheme("spanAttribute", ":", Collections.singletonList("http.method"))
        );

        NamingScheme actual = NamingScheme.createDecisionChain(schemes);
        assertNotNull(actual);
        assertTrue(actual instanceof SpanAttributeNamingScheme);
    }

    @Test
    void verifyThatSchemeOrderIsRetained() {
        List<TransactionNamingScheme> schemes = Arrays.asList(
                new TransactionNamingScheme("spanAttribute", ":", Collections.singletonList("http.method")),
                new TransactionNamingScheme("spanAttribute", "-", Collections.singletonList("http.method"))
        );

        NamingScheme actual = NamingScheme.createDecisionChain(schemes);
        assertNotNull(actual);

        assertEquals(":", ((SpanAttributeNamingScheme)actual).getDelimiter());
        assertEquals("-", ((SpanAttributeNamingScheme)actual.next).getDelimiter());
    }
}