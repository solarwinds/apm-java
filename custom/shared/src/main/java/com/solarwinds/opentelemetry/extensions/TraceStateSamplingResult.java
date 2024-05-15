/*
 * Copyright SolarWinds Worldwide, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solarwinds.opentelemetry.extensions;

import static com.solarwinds.opentelemetry.extensions.SamplingUtil.SW_XTRACE_OPTIONS_RESP_KEY;

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
