package com.appoptics.opentelemetry.extensions;

import com.tracelytics.joboe.XTraceOptions;
import io.opentelemetry.context.ContextKey;

import javax.annotation.concurrent.Immutable;

@Immutable
final class TriggerTraceContextKey {
    static final ContextKey<XTraceOptions> KEY = ContextKey.named("sw-trigger-trace-key");
    static final ContextKey<String> XTRACE_OPTIONS = ContextKey.named("xtrace-options");
    static final ContextKey<String> XTRACE_OPTIONS_SIGNATURE = ContextKey.named("xtrace-options-signature");

    private TriggerTraceContextKey() { }
}

