package com.solarwinds.opentelemetry.extensions;

import com.solarwinds.joboe.sampling.XTraceOptions;
import io.opentelemetry.context.ContextKey;

final class TriggerTraceContextKey {
  public static final ContextKey<XTraceOptions> KEY = ContextKey.named("sw-trigger-trace-key");
  public static final ContextKey<String> XTRACE_OPTIONS = ContextKey.named("xtrace-options");
  public static final ContextKey<String> XTRACE_OPTIONS_SIGNATURE =
      ContextKey.named("xtrace-options-signature");

  private TriggerTraceContextKey() {}
}
