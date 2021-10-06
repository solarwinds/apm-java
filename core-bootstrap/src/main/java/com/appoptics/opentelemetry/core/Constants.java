package com.appoptics.opentelemetry.core;

public interface Constants {
    String SW_KEY_PREFIX = "sw.";
    String OT_KEY_PREFIX = "ot.";
    String SW_INTERNAL_ATTRIBUTE_PREFIX = SW_KEY_PREFIX + "internal.";
    String SW_DETAILED_TRACING = SW_INTERNAL_ATTRIBUTE_PREFIX + "detailedTracing";
    String SW_METRICS = SW_INTERNAL_ATTRIBUTE_PREFIX + "metrics";
    String SW_SAMPLER = SW_INTERNAL_ATTRIBUTE_PREFIX + "sampler";
    String W3C_KEY_PREFIX = "w3c.";
    String SW_UPSTREAM_TRACESTATE = SW_KEY_PREFIX + W3C_KEY_PREFIX + "tracestate";
    String SW_PARENT_ID = SW_KEY_PREFIX + "parentid";
}
