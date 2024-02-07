package com.appoptics.opentelemetry.extensions;

import static com.appoptics.opentelemetry.extensions.SamplingUtil.SW_XTRACE_OPTIONS_RESP_KEY;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.TraceStateBuilder;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

/**
 * A SamplingResult wrapper offering the `sw=spanIdPlaceHolder` tracestate and additional attributes
 */
public class TraceStateSamplingResult implements SamplingResult {
  private final SamplingResult delegated;
  private final Attributes additionalAttributes;
  private final String sanitizedXtraceOptionsResponse;

  private TraceStateSamplingResult(
      SamplingResult delegated, Attributes additionalAttributes, String xtraceOptionsResponse) {
    this.delegated = delegated;
    this.additionalAttributes = additionalAttributes;
    this.sanitizedXtraceOptionsResponse = sanitize(xtraceOptionsResponse);
  }

  public static SamplingResult wrap(
      SamplingResult result, Attributes additionalAttributes, String xtraceOptionsResponse) {
    return new TraceStateSamplingResult(result, additionalAttributes, xtraceOptionsResponse);
  }

  @Override
  public SamplingDecision getDecision() {
    return delegated.getDecision();
  }

  @Override
  public Attributes getAttributes() {
    return delegated.getAttributes().toBuilder().putAll(additionalAttributes).build();
  }

  @Override
  public TraceState getUpdatedTraceState(TraceState parentTraceState) {
    TraceStateBuilder builder = parentTraceState.toBuilder();
    if (sanitizedXtraceOptionsResponse != null) {
      /*
        This is used to store xtrace options response to make it available for injecting as HTTP
        headers for servlet instrumentations. It's not expected to happen in GRPC context. When
        it does, it will cause `tracestate` key to have two values with the other coming from {@link AppOpticsContextPropagator#inject}
      */
      builder.put(SW_XTRACE_OPTIONS_RESP_KEY, sanitizedXtraceOptionsResponse);
    }
    return builder.build();
  }

  /**
   * The tracestate of the W3C trace context is used to piggyback the trigger trace options response
   * header. However, the tracestate value has a more strict character set which doesn't allow `,`
   * or `=`. This method converts `,` to "...." and `=` to "####". There is a minimal chance that
   * the trigger trace header contains the substring "...." or "####" and they will be replaced
   * mistakenly. However, we think it's very rare, and we could ignore the case.
   */
  private static String sanitize(String in) {
    if (in == null) {
      return in;
    }
    return in.replace("=", "####").replace(",", "....");
  }
}
