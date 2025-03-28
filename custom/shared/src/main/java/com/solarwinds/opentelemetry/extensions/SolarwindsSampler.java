/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
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

import static com.solarwinds.joboe.sampling.TraceDecisionUtil.shouldTraceRequest;
import static com.solarwinds.opentelemetry.extensions.SamplingUtil.SW_TRACESTATE_KEY;
import static com.solarwinds.opentelemetry.extensions.SamplingUtil.addXtraceOptionsToAttribute;
import static com.solarwinds.opentelemetry.extensions.SharedNames.LAYER_NAME_PLACEHOLDER;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.TraceDecision;
import com.solarwinds.joboe.sampling.XTraceOptions;
import com.solarwinds.joboe.sampling.XTraceOptionsResponse;
import com.solarwinds.opentelemetry.core.Constants;
import com.solarwinds.opentelemetry.core.Util;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import java.util.Arrays;
import java.util.List;

/**
 * Sampler that uses trace decision logic from our joboe core (consult local and remote settings)
 *
 * <p>Also inject various Solarwinds specific sampling KVs into the `SampleResult`
 */
public class SolarwindsSampler implements Sampler {
  private static final SamplingResult PARENT_SAMPLED =
      SamplingResult.create(
          SamplingDecision.RECORD_AND_SAMPLE,
          Attributes.of(
              AttributeKey.booleanKey(Constants.SW_DETAILED_TRACING), true,
              AttributeKey.booleanKey(Constants.SW_METRICS), true,
              AttributeKey.booleanKey(Constants.SW_SAMPLER), true));
  private static final SamplingResult PARENT_NOT_SAMPLED =
      SamplingResult.create(
          SamplingDecision.DROP,
          Attributes.of(
              AttributeKey.booleanKey(Constants.SW_DETAILED_TRACING), false,
              AttributeKey.booleanKey(Constants.SW_METRICS), false,
              AttributeKey.booleanKey(Constants.SW_SAMPLER), true));

  public static final SamplingResult METRICS_ONLY =
      SamplingResult.create(
          SamplingDecision.RECORD_ONLY,
          Attributes.of(
              AttributeKey.booleanKey(Constants.SW_DETAILED_TRACING), false,
              AttributeKey.booleanKey(Constants.SW_METRICS), true,
              AttributeKey.booleanKey(Constants.SW_SAMPLER), true));

  public static final SamplingResult NOT_TRACED =
      SamplingResult.create(
          SamplingDecision.DROP,
          Attributes.of(
              AttributeKey.booleanKey(Constants.SW_DETAILED_TRACING), false,
              AttributeKey.booleanKey(Constants.SW_METRICS), false,
              AttributeKey.booleanKey(Constants.SW_SAMPLER), true));

  private static final Logger logger = LoggerFactory.getLogger();

  public SolarwindsSampler() {
    logger.info("Attached Solarwinds' Sampler");
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {
    final SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
    final TraceState traceState =
        parentSpanContext.getTraceState() != null
            ? parentSpanContext.getTraceState()
            : TraceState.getDefault();

    final SamplingResult samplingResult;
    final AttributesBuilder additionalAttributesBuilder = Attributes.builder();
    final XTraceOptions xTraceOptions = parentContext.get(TriggerTraceContextKey.KEY);

    String xtraceOptionsResponseStr = null;
    List<String> signals =
        Arrays.asList(
            constructUrl(attributes), String.format(LAYER_NAME_PLACEHOLDER, spanKind, name.trim()));

    if (!parentSpanContext.isValid()) { // no valid traceparent, it is a new trace
      TraceDecision traceDecision = shouldTraceRequest(name, null, xTraceOptions, signals);
      samplingResult = toOtSamplingResult(traceDecision, xTraceOptions, true);
      XTraceOptionsResponse xtraceOptionsResponse =
          XTraceOptionsResponse.computeResponse(xTraceOptions, traceDecision, true);

      if (xtraceOptionsResponse != null) {
        xtraceOptionsResponseStr = xtraceOptionsResponse.toString();
      }

    } else if (parentSpanContext.isRemote()) {
      final String swTraceState = traceState.get(SW_TRACESTATE_KEY);

      if (SamplingUtil.isValidSwTraceState(swTraceState)) { // pass through for request counting
        additionalAttributesBuilder.put(Constants.SW_PARENT_ID, swTraceState.split("-")[0]);
        final String xTraceId = Util.w3cContextToHexString(parentSpanContext);
        final TraceDecision traceDecision =
            shouldTraceRequest(name, xTraceId, xTraceOptions, signals);

        samplingResult = toOtSamplingResult(traceDecision, xTraceOptions, false);
        final XTraceOptionsResponse xTraceOptionsResponse =
            XTraceOptionsResponse.computeResponse(xTraceOptions, traceDecision, false);

        if (xTraceOptionsResponse != null) {
          xtraceOptionsResponseStr = xTraceOptionsResponse.toString();
        }

      } else { // no swTraceState, treat it as a new trace
        final TraceDecision traceDecision = shouldTraceRequest(name, null, xTraceOptions, signals);
        samplingResult = toOtSamplingResult(traceDecision, xTraceOptions, true);

        final XTraceOptionsResponse xTraceOptionsResponse =
            XTraceOptionsResponse.computeResponse(xTraceOptions, traceDecision, true);
        if (xTraceOptionsResponse != null) {
          xtraceOptionsResponseStr = xTraceOptionsResponse.toString();
        }
      }

      final String traceStateValue = parentContext.get(TraceStateKey.KEY);
      if (traceStateValue != null) {
        additionalAttributesBuilder.put(Constants.SW_UPSTREAM_TRACESTATE, traceStateValue);
      }

    } else { // local span, continue with parent based sampling
      samplingResult =
          Sampler.parentBased(Sampler.alwaysOff())
              .shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    SamplingResult result =
        TraceStateSamplingResult.wrap(
            samplingResult, additionalAttributesBuilder.build(), xtraceOptionsResponseStr);

    logger.trace(String.format("Sampling decision: %s", result.getDecision()));
    return result;
  }

  String constructUrl(Attributes attributes) {
    String scheme = attributes.get(UrlAttributes.URL_SCHEME);
    String host = attributes.get(ServerAttributes.SERVER_ADDRESS);
    String path = attributes.get(UrlAttributes.URL_PATH);

    String url = attributes.get(UrlAttributes.URL_FULL);
    if (url == null) {
      StringBuilder builder = new StringBuilder();
      if (scheme != null) {
        builder.append(scheme).append("://");
      }

      if (host != null) {
        builder.append(host);
      }

      if (path != null) {
        builder.append(path);
      }

      url = builder.toString();
    }

    logger.trace(String.format("Constructed url: %s", url));
    return url;
  }

  @Override
  public String getDescription() {
    return "Solarwinds Observability Sampler";
  }

  SamplingResult toOtSamplingResult(
      TraceDecision traceDecision, XTraceOptions xtraceOptions, boolean genesis) {
    SamplingResult result = NOT_TRACED;

    if (traceDecision.isSampled()) {
      final SamplingDecision samplingDecision = SamplingDecision.RECORD_AND_SAMPLE;
      final AttributesBuilder attributesBuilder = Attributes.builder();
      attributesBuilder.put(
          Constants.SW_KEY_PREFIX + "SampleRate", traceDecision.getTraceConfig().getSampleRate());
      attributesBuilder.put(
          Constants.SW_KEY_PREFIX + "SampleSource",
          traceDecision.getTraceConfig().getSampleRateSourceValue());
      attributesBuilder.put(
          Constants.SW_KEY_PREFIX + "BucketRate",
          traceDecision
              .getTraceConfig()
              .getBucketRate(traceDecision.getRequestType().getBucketType()));
      attributesBuilder.put(
          Constants.SW_KEY_PREFIX + "BucketCapacity",
          traceDecision
              .getTraceConfig()
              .getBucketCapacity(traceDecision.getRequestType().getBucketType()));
      attributesBuilder.put(
          Constants.SW_KEY_PREFIX + "RequestType", traceDecision.getRequestType().name());
      attributesBuilder.put(Constants.SW_DETAILED_TRACING, traceDecision.isSampled());
      attributesBuilder.put(Constants.SW_METRICS, traceDecision.isReportMetrics());
      attributesBuilder.put(Constants.SW_SAMPLER, true); // mark that it has been sampled by us

      if (genesis) {
        addXtraceOptionsToAttribute(traceDecision, xtraceOptions, attributesBuilder);
      }
      result = SamplingResult.create(samplingDecision, attributesBuilder.build());
    } else {
      if (traceDecision.isReportMetrics()) {
        result = METRICS_ONLY;
      }
    }
    return result;
  }
}
