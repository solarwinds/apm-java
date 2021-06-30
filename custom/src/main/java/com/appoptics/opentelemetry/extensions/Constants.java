package com.appoptics.opentelemetry.extensions;

public interface Constants {
    String AO_INTERNAL_ATTRIBUTE_PREFIX = "ao.internal.";
    String AO_DETAILED_TRACING = AO_INTERNAL_ATTRIBUTE_PREFIX + "detailedTracing";
    String AO_METRICS = AO_INTERNAL_ATTRIBUTE_PREFIX + "metrics";
    String AO_SAMPLER = AO_INTERNAL_ATTRIBUTE_PREFIX + "sampler";
    String AO_KEY_PREFIX = "ao.";
    String OT_KEY_PREFIX = "ot.";
}
