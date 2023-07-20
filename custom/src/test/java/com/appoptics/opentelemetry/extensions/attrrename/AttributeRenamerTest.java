package com.appoptics.opentelemetry.extensions.attrrename;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AttributeRenamerTest {
    @BeforeAll
    static void setup() {
        Map<String, String> map = new HashMap<>() {{
            put("old_key_0", "new_key_0");
            put("old_key_1", "new_key_1");
        }};
        AttributeRenamer.initialize(map);
    }

    @Test
    void returnNewAttributeGiveOldName() {
        String expected = "new_key_0";
        String actual = AttributeRenamer.getInstance().rename("old_key_0");

        assertEquals(expected, actual);
    }

    @Test
    void returnAttributesWithNewName() {
        Attributes attributes = Attributes.builder()
                .put("old_key_0", "hello")
                .put("old_key_1", "world")
                .build();

        Attributes renamed = AttributeRenamer.getInstance().rename(attributes);
        assertEquals("hello", renamed.get(AttributeKey.stringKey("new_key_0")));
        assertEquals("world", renamed.get(AttributeKey.stringKey("new_key_1")));

        assertNull(renamed.get(AttributeKey.stringKey("old_key_0")));
        assertNull(renamed.get(AttributeKey.stringKey("old_key_1")));
    }
}