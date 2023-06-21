package com.appoptics.opentelemetry.extensions.initialize.config;

import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.joboe.config.LogTraceIdScope;
import com.tracelytics.joboe.config.LogTraceIdSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LogTraceIdSettingParserTest {

    @InjectMocks
    private LogTraceIdSettingParser tested;

    @Test
    void returnLogTraceIdSettingGivenValidJsonConfiguration() throws InvalidConfigException {
        String json = "{" +
                "\"autoInsert\": \"sampledOnly\"," +
                "\"mdc\":\"enabled\"," +
                "}";

        LogTraceIdSetting actual = tested.convert(json);
        LogTraceIdSetting expected = new LogTraceIdSetting(LogTraceIdScope.SAMPLED_ONLY,
                LogTraceIdScope.ENABLED);

        assertEquals(expected.getAutoInsertScope(), actual.getAutoInsertScope());
        assertEquals(expected.getMdcScope(), actual.getMdcScope());
    }


    @Test
    void throwInvalidConfigExceptionGivenUnknownKeyInJsonConfiguration() throws InvalidConfigException {
        String json = "{" +
                "\"autoInsert\": \"sampledOnly\"," +
                "\"mdc\":\"enabled\"," +
                "\"riddle-me-this\":\"to duck or not two ducks\"," +
                "}";

        assertThrows(InvalidConfigException.class, () -> tested.convert(json));
    }
}