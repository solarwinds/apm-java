package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.core.Constants;
import com.appoptics.opentelemetry.core.Util;
import com.google.auto.service.AutoService;
import com.tracelytics.joboe.TraceDecision;
import com.tracelytics.joboe.XTraceOptions;
import com.tracelytics.joboe.XTraceOptionsResponse;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

import static com.appoptics.opentelemetry.extensions.AppOpticsSpanExporter.LAYER_FORMAT;
import static com.appoptics.opentelemetry.extensions.TraceStateSamplingResult.SW_SPAN_PLACEHOLDER;
import static com.appoptics.opentelemetry.extensions.TraceStateSamplingResult.SW_TRACESTATE_KEY;
import static com.tracelytics.joboe.TraceDecisionUtil.shouldTraceRequest;

/**
 * Sampler that uses trace decision logic from our joboe core (consult local and remote settings)
 * <p>
 * Also inject various AppOptics specific sampling KVs into the `SampleResult`
 */
@AutoService(Sampler.class)
public class AppOpticsSampler implements Sampler {
    private final SamplingResult PARENT_SAMPLED = SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE,
            Attributes.of(
                    AttributeKey.booleanKey(Constants.SW_DETAILED_TRACING), true,
                    AttributeKey.booleanKey(Constants.SW_METRICS), true,
                    AttributeKey.booleanKey(Constants.SW_SAMPLER), true));
    private final SamplingResult PARENT_NOT_SAMPLED = SamplingResult.create(SamplingDecision.DROP,
            Attributes.of(
                    AttributeKey.booleanKey(Constants.SW_DETAILED_TRACING), false,
                    AttributeKey.booleanKey(Constants.SW_METRICS), false,
                    AttributeKey.booleanKey(Constants.SW_SAMPLER), true));

    private final SamplingResult METRICS_ONLY = SamplingResult.create(SamplingDecision.RECORD_ONLY,
            Attributes.of(
                    AttributeKey.booleanKey(Constants.SW_DETAILED_TRACING), false,
                    AttributeKey.booleanKey(Constants.SW_METRICS), true,
                    AttributeKey.booleanKey(Constants.SW_SAMPLER), true
            ));

    private final SamplingResult NOT_TRACED = SamplingResult.create(SamplingDecision.DROP,
            Attributes.of(
                    AttributeKey.booleanKey(Constants.SW_DETAILED_TRACING), false,
                    AttributeKey.booleanKey(Constants.SW_METRICS), false,
                    AttributeKey.booleanKey(Constants.SW_SAMPLER), true));

    private static final Logger logger = LoggerFactory.getLogger();

    @Override
    public SamplingResult shouldSample(@Nonnull Context parentContext,
                                       @Nonnull String traceId,
                                       @Nonnull String name,
                                       @Nonnull SpanKind spanKind,
                                       @Nonnull Attributes attributes,
                                       @Nonnull List<LinkData> parentLinks) {
        final SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
        final TraceState traceState = parentSpanContext.getTraceState() != null ? parentSpanContext.getTraceState() : TraceState.getDefault();

        final SamplingResult samplingResult;
        final AttributesBuilder additionalAttributesBuilder = Attributes.builder();
        final XTraceOptions xTraceOptions = parentContext.get(TriggerTraceContextKey.KEY);

        String xTraceOptionsResponseStr = null;
        List<String> signals = Arrays.asList(constructUrl(attributes), String.format(LAYER_FORMAT, spanKind, name.trim()));

        if (!parentSpanContext.isValid()) { // no valid traceparent, it is a new trace
            TraceDecision traceDecision = shouldTraceRequest(name, null, xTraceOptions, signals);
            samplingResult = toOtSamplingResult(traceDecision);
            XTraceOptionsResponse xTraceOptionsResponse = XTraceOptionsResponse.computeResponse(xTraceOptions,
                    traceDecision, true);

            if (xTraceOptionsResponse != null) {
                xTraceOptionsResponseStr = xTraceOptionsResponse.toString();
            }

        } else {
            final String swVal = traceState.get(SW_TRACESTATE_KEY);
            String parentId = null;
            if (!isValidSWTraceStateKey(swVal)) { // broken or non-exist sw tracestate, treat it as a new trace
                final TraceDecision traceDecision = shouldTraceRequest(name, null, xTraceOptions, signals);
                samplingResult = toOtSamplingResult(traceDecision);
                final XTraceOptionsResponse xTraceOptionsResponse = XTraceOptionsResponse.computeResponse(xTraceOptions,
                        traceDecision, true);
                if (xTraceOptionsResponse != null) {
                    xTraceOptionsResponseStr = xTraceOptionsResponse.toString();
                }
            } else { // follow the upstream sw trace decision
                final TraceFlags traceFlags = TraceFlags.fromByte(swVal.split("-")[1].getBytes()[1]);
                if (parentSpanContext.isRemote()) { // root span needs to roll the dice
                    final String xTraceId = Util.w3CContextToHexString(parentSpanContext);
                    final TraceDecision traceDecision = shouldTraceRequest(name, xTraceId, xTraceOptions, signals);
                    samplingResult = toOtSamplingResult(traceDecision);

                    final XTraceOptionsResponse xTraceOptionsResponse = XTraceOptionsResponse.computeResponse(
                            xTraceOptions, traceDecision, true);
                    if (xTraceOptionsResponse != null) {
                        xTraceOptionsResponseStr = xTraceOptionsResponse.toString();
                    }
                } else { // non-root span just follows the parent span's decision
                    samplingResult = (traceFlags.isSampled() ? Sampler.alwaysOn() : Sampler.alwaysOff())
                            .shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
                }
                parentId = swVal.split("-")[0];
            }
            if (parentSpanContext.isRemote() && parentId != null) {
                additionalAttributesBuilder.put(Constants.SW_PARENT_ID, parentId);
            }
        }

        if (parentSpanContext.isRemote() && !traceState.isEmpty()) {
            final String traceStateValue = parentContext.get(TraceStateKey.KEY);
            if (traceStateValue != null) {
                additionalAttributesBuilder.put(Constants.SW_UPSTREAM_TRACESTATE, traceStateValue);
            }
        }
        SamplingResult result = TraceStateSamplingResult.wrap(samplingResult, additionalAttributesBuilder.build(),
                xTraceOptionsResponseStr);

        logger.debug(String.format("Sampling decision: %s", result.getDecision()));
        return result;
    }

    private boolean isValidSWTraceStateKey(String swVal) {
        if (swVal == null || !swVal.contains("-")) {
            return false;
        }
        final String[] swTraceState = swVal.split("-");
        if (swTraceState.length != 2) {
            return false;
        }

        return (swTraceState[0].equals(
                SW_SPAN_PLACEHOLDER) || swTraceState[0].length() == 16) // 16 is the HEXLENGTH of the Otel trace id
                && (swTraceState[1].equals("00") || swTraceState[1].equals("01"));
    }

    private String constructUrl(Attributes attributes) {
        String scheme = attributes.get(SemanticAttributes.HTTP_SCHEME);
        String host = attributes.get(SemanticAttributes.NET_HOST_NAME);
        String target = attributes.get(SemanticAttributes.HTTP_TARGET);

        String url = String.format("%s://%s%s", scheme, host, target);
        logger.debug(String.format("Constructed url: %s", url));
        return url;
    }

    @Override
    public String getDescription() {
        return "Solarwinds Observability Sampler";
    }

    private SamplingResult toOtSamplingResult(TraceDecision aoTraceDecision) {
        SamplingResult result = NOT_TRACED;

        if (aoTraceDecision.isSampled()) {
            final SamplingDecision samplingDecision = SamplingDecision.RECORD_AND_SAMPLE;
            final AttributesBuilder aoAttributesBuilder = Attributes.builder();
            aoAttributesBuilder.put(Constants.SW_KEY_PREFIX + "SampleRate",
                    aoTraceDecision.getTraceConfig().getSampleRate());
            aoAttributesBuilder.put(Constants.SW_KEY_PREFIX + "SampleSource",
                    aoTraceDecision.getTraceConfig().getSampleRateSourceValue());
            aoAttributesBuilder.put(Constants.SW_KEY_PREFIX + "BucketRate",
                    aoTraceDecision.getTraceConfig().getBucketRate(aoTraceDecision.getRequestType().getBucketType()));
            aoAttributesBuilder.put(Constants.SW_KEY_PREFIX + "BucketCapacity",
                    aoTraceDecision.getTraceConfig().getBucketCapacity(
                            aoTraceDecision.getRequestType().getBucketType()));
            aoAttributesBuilder.put(Constants.SW_KEY_PREFIX + "RequestType", aoTraceDecision.getRequestType().name());
            aoAttributesBuilder.put(Constants.SW_DETAILED_TRACING, aoTraceDecision.isSampled());
            aoAttributesBuilder.put(Constants.SW_METRICS, aoTraceDecision.isReportMetrics());
            aoAttributesBuilder.put(Constants.SW_SAMPLER, true); //mark that it has been sampled by us

            result = SamplingResult.create(samplingDecision, aoAttributesBuilder.build());
        } else {
            if (aoTraceDecision.isReportMetrics()) {
                result = METRICS_ONLY;
            }
        }
        return result;
    }
}
