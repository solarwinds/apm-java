package com.appoptics.opentelemetry.extensions;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

/**
 * A SamplingResult wrapper offering the `sw=spanIdPlaceHolder` tracestate
 */
public class TraceStateSamplingResult implements SamplingResult {
    public static final String SW_TRACESTATE_KEY = "sw";
    public static final String SW_SPAN_PLACEHOLDER = "SWSpanIdPlaceHolder";
    public static final String SW_SPAN_PLACEHOLDER_SAMPLED = SW_SPAN_PLACEHOLDER + "-01";
    public static final String SW_SPAN_PLACEHOLDER_NOT_SAMPLED = SW_SPAN_PLACEHOLDER + "-00";
    private final SamplingResult delegated;
    private final String swTraceState;

    private TraceStateSamplingResult(SamplingResult delegated) {
        this.delegated = delegated;
        this.swTraceState = (delegated.getDecision() == SamplingDecision.RECORD_AND_SAMPLE
                ? SW_SPAN_PLACEHOLDER_SAMPLED : SW_SPAN_PLACEHOLDER_NOT_SAMPLED);
    }

    public static SamplingResult wrap(SamplingResult result) {
        return new TraceStateSamplingResult(result);
    }

    @Override
    public SamplingDecision getDecision() {
        return delegated.getDecision();
    }

    @Override
    public Attributes getAttributes() {
        return delegated.getAttributes();
    }

    @Override
    public TraceState getUpdatedTraceState(TraceState parentTraceState) {
        return parentTraceState.toBuilder().put(SW_TRACESTATE_KEY, swTraceState).build();
    }
}
