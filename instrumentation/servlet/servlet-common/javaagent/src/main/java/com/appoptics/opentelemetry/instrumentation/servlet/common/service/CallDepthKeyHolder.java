package com.appoptics.opentelemetry.instrumentation.servlet.common.service;

public class CallDepthKeyHolder {
    public static Class<?> getCallDepthKey() {
        class Key {}
        return Key.class;
    }
}
