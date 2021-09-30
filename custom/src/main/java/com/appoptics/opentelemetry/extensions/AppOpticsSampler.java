package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.core.Constants;
import com.appoptics.opentelemetry.core.Util;
import com.google.auto.service.AutoService;
import com.tracelytics.joboe.TraceDecision;
import com.tracelytics.joboe.TraceDecisionUtil;
import com.tracelytics.joboe.XTraceOptions;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import java.util.List;
import java.util.stream.Collectors;

import static com.appoptics.opentelemetry.extensions.TraceStateSamplingResult.*;

/**
 * Sampler that uses trace decision logic from our joboe core (consult local and remote settings)
 *
 * Also inject various AppOptics specific sampling KVs into the `SampleResult`
 */
@AutoService(Sampler.class)
public class AppOpticsSampler implements Sampler {
    private static final String SW_UPSTREAM_VENDORS = "ao.UpstreamTraceVendors";
    private static final String SW_PARENT_ID = "ao.SWParentID";
    private final SamplingResult PARENT_SAMPLED = SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE,
            Attributes.of(
                    AttributeKey.booleanKey(Constants.AO_DETAILED_TRACING), true,
                    AttributeKey.booleanKey(Constants.AO_METRICS), true,
                    AttributeKey.booleanKey(Constants.AO_SAMPLER), true));
    private final SamplingResult PARENT_NOT_SAMPLED = SamplingResult.create(SamplingDecision.DROP,
            Attributes.of(
                    AttributeKey.booleanKey(Constants.AO_DETAILED_TRACING), false,
                    AttributeKey.booleanKey(Constants.AO_METRICS), false,
                    AttributeKey.booleanKey(Constants.AO_SAMPLER), true));

    private final SamplingResult METRICS_ONLY = SamplingResult.create(SamplingDecision.RECORD_ONLY,
            Attributes.of(
                    AttributeKey.booleanKey(Constants.AO_DETAILED_TRACING), false,
                    AttributeKey.booleanKey(Constants.AO_METRICS), true,
                    AttributeKey.booleanKey(Constants.AO_SAMPLER), true
            ));

    private final SamplingResult NOT_TRACED = SamplingResult.create(SamplingDecision.DROP,
            Attributes.of(
                    AttributeKey.booleanKey(Constants.AO_DETAILED_TRACING), false,
                    AttributeKey.booleanKey(Constants.AO_METRICS), false,
                    AttributeKey.booleanKey(Constants.AO_SAMPLER), true));

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind, Attributes attributes, List<LinkData> parentLinks) {
        SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
        TraceState traceState = parentSpanContext.getTraceState() != null ? parentSpanContext.getTraceState() : TraceState.getDefault();
        String resource = getResource(attributes);
        SamplingResult samplingResult;
        AttributesBuilder additionalAttributesBuilder = Attributes.builder();
        XTraceOptions xTraceOptions = parentContext.get(TriggerTraceContextKey.KEY);

        if (!parentSpanContext.isValid()) { // no valid traceparent, it is a new trace
            samplingResult = toOtSamplingResult(TraceDecisionUtil.shouldTraceRequest(name, null, xTraceOptions, resource));
        } else {
            String swVal = traceState.get(SW_TRACESTATE_KEY);
            String parentId = null;
            if (!isValidSWTraceStateKey(swVal)) { // broken or non-exist sw tracestate, treat it as a new trace
                samplingResult = toOtSamplingResult(TraceDecisionUtil.shouldTraceRequest(name, null, xTraceOptions, resource));
            } else { // follow the upstream sw trace decision
                TraceFlags traceFlags = TraceFlags.fromByte(swVal.split("-")[1].getBytes()[1]);
                if (parentSpanContext.isRemote()) { // root span needs to roll the dice
                    String xTraceId = Util.buildXTraceId(parentSpanContext);
                    TraceDecision aoTraceDecision = TraceDecisionUtil.shouldTraceRequest(name, xTraceId, xTraceOptions, resource);
                    samplingResult = toOtSamplingResult(aoTraceDecision);
                } else { // non-root span just follows the parent span's decision
                    samplingResult = (traceFlags.isSampled() ? Sampler.alwaysOn() : Sampler.alwaysOff())
                            .shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
                }
                parentId = swVal.split("-")[0];
            }
            if (parentSpanContext.isRemote() && parentId != null) {
                additionalAttributesBuilder.put(SW_PARENT_ID, parentId);
            }
        }

        if (parentSpanContext.isRemote() && !traceState.isEmpty()) {
            String upstreamVendors = traceState.asMap()
                    .keySet()
                    .stream()
                    .filter(key->!key.equals(SW_TRACESTATE_KEY))
                    .collect(Collectors.joining(","));
            if (!upstreamVendors.isEmpty()) {
                additionalAttributesBuilder.put(SW_UPSTREAM_VENDORS, upstreamVendors);
            }
        }
        return TraceStateSamplingResult.wrap(samplingResult, additionalAttributesBuilder.build());
    }

    private boolean isValidSWTraceStateKey(String swVal) {
        if (swVal == null || !swVal.contains("-")) {
            return false;
        }
        String[] swTraceState = swVal.split("-");
        if (swTraceState.length != 2) {
            return false;
        }

        return (swTraceState[0].equals(SW_SPAN_PLACEHOLDER) || swTraceState[0].length() == 16) // 16 is the HEXLENGTH of the Otel trace id
                && (swTraceState[1].equals("00") || swTraceState[1].equals("01"));
    }

    private String getResource(Attributes attributes) {
        return Util.parsePath(attributes.get(SemanticAttributes.HTTP_URL));
    }

    @Override
    public String getDescription() {
        return "AppOptics Sampler";
    }

    private SamplingResult toOtSamplingResult(TraceDecision aoTraceDecision) {
        SamplingResult result = NOT_TRACED;

        if (aoTraceDecision.isSampled()) {
            SamplingDecision samplingDecision = SamplingDecision.RECORD_AND_SAMPLE;
            AttributesBuilder aoAttributesBuilder = Attributes.builder();
            aoAttributesBuilder.put(Constants.AO_KEY_PREFIX + "SampleRate", aoTraceDecision.getTraceConfig().getSampleRate());
            aoAttributesBuilder.put(Constants.AO_KEY_PREFIX + "SampleSource", aoTraceDecision.getTraceConfig().getSampleRateSourceValue());
            aoAttributesBuilder.put(Constants.AO_KEY_PREFIX + "BucketRate", aoTraceDecision.getTraceConfig().getBucketRate(aoTraceDecision.getRequestType().getBucketType()));
            aoAttributesBuilder.put(Constants.AO_KEY_PREFIX + "BucketCapacity", aoTraceDecision.getTraceConfig().getBucketCapacity(aoTraceDecision.getRequestType().getBucketType()));
            aoAttributesBuilder.put(Constants.AO_KEY_PREFIX + "RequestType", aoTraceDecision.getRequestType().name());
            aoAttributesBuilder.put(Constants.AO_DETAILED_TRACING, aoTraceDecision.isSampled());
            aoAttributesBuilder.put(Constants.AO_METRICS, aoTraceDecision.isReportMetrics());
            aoAttributesBuilder.put(Constants.AO_SAMPLER, true); //mark that it has been sampled by us

            result = SamplingResult.create(samplingDecision, aoAttributesBuilder.build());
        } else {
            if (aoTraceDecision.isReportMetrics()) {
                result = METRICS_ONLY; // is this correct? probably not...
            }
        }
        return result;
    }
}
