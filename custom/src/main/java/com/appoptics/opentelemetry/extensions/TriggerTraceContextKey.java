package com.appoptics.opentelemetry.extensions;

import com.tracelytics.joboe.XTraceOptions;
import io.opentelemetry.context.ContextKey;
import javax.annotation.concurrent.Immutable;

@Immutable
final class TriggerTraceContextKey {
  public static final ContextKey<XTraceOptions> KEY = ContextKey.named("sw-trigger-trace-key");
  public static final ContextKey<String> XTRACE_OPTIONS = ContextKey.named("xtrace-options");
  public static final ContextKey<String> XTRACE_OPTIONS_SIGNATURE =
      ContextKey.named("xtrace-options-signature");

  private TriggerTraceContextKey() {}
}
