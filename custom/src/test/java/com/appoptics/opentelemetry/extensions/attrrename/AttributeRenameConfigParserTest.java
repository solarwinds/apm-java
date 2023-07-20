package com.appoptics.opentelemetry.extensions.attrrename;

import com.tracelytics.joboe.config.InvalidConfigException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AttributeRenameConfigParserTest {
    @InjectMocks
    private AttributeRenameConfigParser tested;

    @Test
    void returnAttributeRenameMapGivenStringRepresentation() throws InvalidConfigException {
        String input = "old_key_0=new_key_0,old_key_1= new_key_1";
        Map<String, String> actual = tested.convert(input);
        Map<String, String> expected = new HashMap<>() {{
            put("old_key_0", "new_key_0");
            put("old_key_1", "new_key_1");
        }};

        assertEquals(expected, actual);
    }

    @Test
    void throwInvalidConfigExceptionGivenInvalidStringRepresentation() throws InvalidConfigException {
        String input = "old_key_0=new_key_0 old_key_1= new_key_1,old_key_1= new_key_1";
        assertThrows(InvalidConfigException.class, () -> tested.convert(input));
    }

    @Test
    void throwInvalidConfigExceptionGivenDuplicateKeyInStringRepresentation() throws InvalidConfigException {
        String input = "old_key_0=new_key_0,old_key_0=new_key_0";
        assertThrows(InvalidConfigException.class, () -> tested.convert(input));
    }

    @Test
    void throwInvalidConfigExceptionGivenNameCollisionInStringRepresentation() throws InvalidConfigException {
        String input = "old_key_0=new_key_0,old_key_1=new_key_0";
        assertThrows(InvalidConfigException.class, () -> tested.convert(input));
    }
}