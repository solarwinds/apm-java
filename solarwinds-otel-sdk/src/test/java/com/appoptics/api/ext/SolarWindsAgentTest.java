package com.appoptics.api.ext;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolarWindsAgentTest {

    @BeforeAll
    static void setup(){
        SolarWindsAgent.setAgentAttachedToFalse();
    }

    @Test
    void verifyThatSetTransactionNameReturnsTrueWhenNoop() {
        assertTrue(SolarWindsAgent.setTransactionName("hello world!"));
    }

    @Test
    void verifyThatWaitUntilAgentReadyReturnsFalseWhenNoop() {
        assertFalse(SolarWindsAgent.waitUntilAgentReady(0, TimeUnit.MILLISECONDS));
    }
}