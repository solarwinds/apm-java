package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.core.Constants;
import com.appoptics.opentelemetry.core.Util;
import com.google.auto.service.AutoService;
import com.tracelytics.joboe.TraceDecision;
import com.tracelytics.joboe.TraceDecisionUtil;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import java.util.List;

@AutoService(Sampler.class)
public class AppOpticsSampler implements Sampler {
    private SamplingResult PARENT_SAMPLED = SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE,
            Attributes.of(
                    AttributeKey.booleanKey(Constants.AO_DETAILED_TRACING), true,
                    AttributeKey.booleanKey(Constants.AO_METRICS), true,
                    AttributeKey.booleanKey(Constants.AO_SAMPLER), true));
    private SamplingResult PARENT_NOT_SAMPLED = SamplingResult.create(SamplingDecision.DROP,
            Attributes.of(
                    AttributeKey.booleanKey(Constants.AO_DETAILED_TRACING), false,
                    AttributeKey.booleanKey(Constants.AO_METRICS), false,
                    AttributeKey.booleanKey(Constants.AO_SAMPLER), true));

    private SamplingResult METRICS_ONLY = SamplingResult.create(SamplingDecision.RECORD_ONLY,
            Attributes.of(
                    AttributeKey.booleanKey(Constants.AO_DETAILED_TRACING), false,
                    AttributeKey.booleanKey(Constants.AO_METRICS), true,
                    AttributeKey.booleanKey(Constants.AO_SAMPLER), true
            ));

    private SamplingResult NOT_TRACED = SamplingResult.create(SamplingDecision.DROP,
            Attributes.of(
                    AttributeKey.booleanKey(Constants.AO_DETAILED_TRACING), false,
                    AttributeKey.booleanKey(Constants.AO_METRICS), false,
                    AttributeKey.booleanKey(Constants.AO_SAMPLER), true));

    //public static TraceDecision shouldTraceRequest(String layer, String inXTraceID, XTraceOptions xTraceOptions, String resource) {
    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind, Attributes attributes, List<LinkData> parentLinks) {
        SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();

        if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) { //then a root span of this service
            String xTraceId = null;
            if (parentSpanContext.isRemote()) {
                xTraceId = Util.buildXTraceId(parentSpanContext);
            }
            String resource = getResource(attributes);
            TraceDecision aoTraceDecision = TraceDecisionUtil.shouldTraceRequest(name, xTraceId, null, resource);
            return toOtSamplingResult(aoTraceDecision);
        } else { //follow parent's decision
            return parentSpanContext.isSampled() ? PARENT_SAMPLED : PARENT_NOT_SAMPLED;
        }
    }

    private String getResource(Attributes attributes) {
        String resource = Util.parsePath(attributes.get(SemanticAttributes.HTTP_URL));
        //TODO consider other resource too like MQ
        return resource;
    }

    @Override
    public String getDescription() {
        return "AppOptics Sampler";
    }

    private SamplingResult toOtSamplingResult(TraceDecision aoTraceDecision) {
        if (aoTraceDecision.isSampled()) {
            SamplingDecision samplingDecision = SamplingDecision.RECORD_AND_SAMPLE;
            AttributesBuilder builder = Attributes.builder();
            builder.put(Constants.AO_KEY_PREFIX + "SampleRate", aoTraceDecision.getTraceConfig().getSampleRate());
            builder.put(Constants.AO_KEY_PREFIX + "SampleSource", aoTraceDecision.getTraceConfig().getSampleRateSourceValue());
            builder.put(Constants.AO_KEY_PREFIX + "BucketRate",aoTraceDecision.getTraceConfig().getBucketRate(aoTraceDecision.getRequestType().getBucketType()));
            builder.put(Constants.AO_KEY_PREFIX + "BucketCapacity",aoTraceDecision.getTraceConfig().getBucketCapacity(aoTraceDecision.getRequestType().getBucketType()));
            builder.put(Constants.AO_KEY_PREFIX + "RequestType", aoTraceDecision.getRequestType().name());
            builder.put(Constants.AO_DETAILED_TRACING, aoTraceDecision.isSampled());
            builder.put(Constants.AO_METRICS, aoTraceDecision.isReportMetrics());
            builder.put(Constants.AO_SAMPLER, true); //mark that it has been sampled by us
            Attributes attributes = builder.build();
            return SamplingResult.create(samplingDecision, attributes);
        } else {
            if (aoTraceDecision.isReportMetrics()) {
                return METRICS_ONLY; // is this correct? probably not...
            } else {
                return NOT_TRACED;
            }
        }
    }
}
