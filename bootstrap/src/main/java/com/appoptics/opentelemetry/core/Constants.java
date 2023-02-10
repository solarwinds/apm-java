package com.appoptics.opentelemetry.core;


public class Constants {
    // FIXME: bad practice to define a collection of constants in either a class or an interface (even worse)
    public static final String SW_KEY_PREFIX = "sw.";
    public static final String OT_KEY_PREFIX = "ot.";
    public static final String SW_INTERNAL_ATTRIBUTE_PREFIX = SW_KEY_PREFIX + "internal.";
    public static final String SW_DETAILED_TRACING = SW_INTERNAL_ATTRIBUTE_PREFIX + "detailedTracing";
    public static final String SW_METRICS = SW_INTERNAL_ATTRIBUTE_PREFIX + "metrics";
    public static final String SW_SAMPLER = SW_INTERNAL_ATTRIBUTE_PREFIX + "sampler";
    public static final String W3C_KEY_PREFIX = "w3c.";
    public static final String SW_UPSTREAM_TRACESTATE = SW_KEY_PREFIX + W3C_KEY_PREFIX + "tracestate";
    public static final String SW_PARENT_ID = SW_KEY_PREFIX + "tracestate_parent_id";
}
