package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.core.Constants;
import com.appoptics.opentelemetry.core.Util;
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
import static com.appoptics.opentelemetry.extensions.SamplingUtil.SW_TRACESTATE_KEY;
import static com.appoptics.opentelemetry.extensions.SamplingUtil.addXtraceOptionsToAttribute;
import static com.appoptics.opentelemetry.extensions.SamplingUtil.isValidSWTraceState;
import static com.tracelytics.joboe.TraceDecisionUtil.shouldTraceRequest;

/**
 * Sampler that uses trace decision logic from our joboe core (consult local and remote settings)
 * <p>
 * Also inject various AppOptics specific sampling KVs into the `SampleResult`
 */
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

    public AppOpticsSampler() {
        logger.info("Attached Solarwinds' Sampler");
    }

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
        List<String> signals = Arrays.asList(constructUrl(attributes),
                String.format(LAYER_FORMAT, spanKind, name.trim()));

        if (!parentSpanContext.isValid()) { // no valid traceparent, it is a new trace
            TraceDecision traceDecision = shouldTraceRequest(name, null, xTraceOptions, signals);
            samplingResult = toOtSamplingResult(traceDecision, xTraceOptions, true);
            XTraceOptionsResponse xTraceOptionsResponse = XTraceOptionsResponse.computeResponse(xTraceOptions,
                    traceDecision, true);

            if (xTraceOptionsResponse != null) {
                xTraceOptionsResponseStr = xTraceOptionsResponse.toString();
            }

        } else {
            final String swVal = traceState.get(SW_TRACESTATE_KEY);
            String parentId = null;
            if (!isValidSWTraceState(swVal)) { // broken or non-exist sw tracestate, treat it as a new trace
                final TraceDecision traceDecision = shouldTraceRequest(name, null, xTraceOptions, signals);
                samplingResult = toOtSamplingResult(traceDecision, xTraceOptions, true);
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
                    samplingResult = toOtSamplingResult(traceDecision, xTraceOptions, false);

                    final XTraceOptionsResponse xTraceOptionsResponse = XTraceOptionsResponse.computeResponse(
                            xTraceOptions, traceDecision, false);
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

        logger.trace(String.format("Sampling decision: %s", result.getDecision()));
        return result;
    }


    private String constructUrl(Attributes attributes) {
        String scheme = attributes.get(SemanticAttributes.HTTP_SCHEME);
        String host = attributes.get(SemanticAttributes.NET_HOST_NAME);
        String target = attributes.get(SemanticAttributes.HTTP_TARGET);

        String url = String.format("%s://%s%s", scheme, host, target);
        logger.trace(String.format("Constructed url: %s", url));
        return url;
    }

    @Override
    public String getDescription() {
        return "Solarwinds Observability Sampler";
    }

    private SamplingResult toOtSamplingResult(TraceDecision traceDecision, XTraceOptions xTraceOptions,
                                              boolean genesis) {
        SamplingResult result = NOT_TRACED;

        if (traceDecision.isSampled()) {
            final SamplingDecision samplingDecision = SamplingDecision.RECORD_AND_SAMPLE;
            final AttributesBuilder attributesBuilder = Attributes.builder();
            attributesBuilder.put(Constants.SW_KEY_PREFIX + "SampleRate",
                    traceDecision.getTraceConfig().getSampleRate());
            attributesBuilder.put(Constants.SW_KEY_PREFIX + "SampleSource",
                    traceDecision.getTraceConfig().getSampleRateSourceValue());
            attributesBuilder.put(Constants.SW_KEY_PREFIX + "BucketRate",
                    traceDecision.getTraceConfig().getBucketRate(traceDecision.getRequestType().getBucketType()));
            attributesBuilder.put(Constants.SW_KEY_PREFIX + "BucketCapacity",
                    traceDecision.getTraceConfig().getBucketCapacity(
                            traceDecision.getRequestType().getBucketType()));
            attributesBuilder.put(Constants.SW_KEY_PREFIX + "RequestType", traceDecision.getRequestType().name());
            attributesBuilder.put(Constants.SW_DETAILED_TRACING, traceDecision.isSampled());
            attributesBuilder.put(Constants.SW_METRICS, traceDecision.isReportMetrics());
            attributesBuilder.put(Constants.SW_SAMPLER, true); //mark that it has been sampled by us

            if (genesis) {
                addXtraceOptionsToAttribute(traceDecision, xTraceOptions, attributesBuilder);
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
