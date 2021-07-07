package com.appoptics.api.ext.impl;

import com.appoptics.api.ext.TraceEvent;
import com.appoptics.api.ext.model.NoOpEvent;
import com.appoptics.api.ext.model.OpenTelemetryTraceEvent;
import com.appoptics.opentelemetry.core.RootSpan;
import com.appoptics.opentelemetry.core.Util;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;

public class TraceHandler implements ITraceHandler {
    private final Logger logger = Logger.getLogger(TraceHandler.class.getName());
    private static final ContextKey<String> AO_SDK_SPAN_ID_CONTEXT_KEY = ContextKey.named("ao.sdk.spanId");
    private static WeakHashMap<Span, Scope> scopeLookup = new WeakHashMap<Span, Scope>();
    private static WeakHashMap<Span, Scope> remoteScopeLookup = new WeakHashMap<Span, Scope>();

    private static final Tracer tracer;

    static {
        tracer = GlobalOpenTelemetry.getTracer("appoptics-sdk");
    }

    /**
     * {@inheritDoc}
     */
    public TraceEvent startTrace(String layer) {
        return startOrContinueTrace(layer, null);
    }
    
    /**
     * {@inheritDoc}
     */
    public TraceEvent continueTrace(String layer, String inXTraceID) {
        return startOrContinueTrace(layer, inXTraceID);
    }

    /**
     * {@inheritDoc}
     */
    public String endTrace(String layer) {
        return endTrace(layer, (Map<String, Object>) null);
    }


    /**
     * {@inheritDoc}
     */
    public String endTrace(String layer, Map<String, Object> info) {
        Span currentSpan = Span.current();
        if (currentSpan == null) {
            logger.warning("Attempt to end a Trace but there's no active span. Ignoring the operation");
            return "";
        }

        if (!validateSpanExit(currentSpan)) {
            return "";
        }

        if (info != null) {
            Util.setSpanAttributes(currentSpan, info);
        }
        currentSpan.end();

        Scope scope = scopeLookup.remove(currentSpan);
        if (scope != null) {
            scope.close();
        }

        Scope remoteScope = remoteScopeLookup.remove(Span.current());
        if (remoteScope != null) {
            remoteScope.close();
        }

        return Util.buildSpanExitMetadata(currentSpan.getSpanContext()).toHexString();
    }

    private boolean validateSpanExit(Span currentSpan) {
        // check if span is the expected SDK span
        String expectedSpanId = Context.current().get(AO_SDK_SPAN_ID_CONTEXT_KEY);
        if (expectedSpanId == null) {
            logger.warning("Attempt to end a SDK span but the active span was not created by SDK : " + currentSpan.getSpanContext());
            return false;
        } else if (!expectedSpanId.equals(currentSpan.getSpanContext().getSpanId())) {
            logger.warning("Attempt to end a SDK span but the active span has span ID " + currentSpan.getSpanContext().getSpanId() + " which is not the same as the expected span ID " + expectedSpanId);
            return false;
        } else {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public String endTrace(String layer, Object... info) {
        return endTrace(layer, Util.keyValuePairsToMap(info));
    }

   
    /**
     * {@inheritDoc}
     */
    public TraceEvent createEntryEvent(String layer) {
        //to adhere to the original behavior, which metadata is captured when event is created
        Span currentSpan = Span.current();
        return new OpenTelemetryTraceEvent(
                event -> {
                    try (Scope parentScope = currentSpan.makeCurrent()){
                        SpanBuilder spanBuilder = tracer.spanBuilder(event.getOperationName());
                        Util.setSpanAttributes(spanBuilder, event.getKeyValues());
                        //TODO https://github.com/open-telemetry/opentelemetry-java/blob/main/QUICKSTART.md#create-spans-with-links, link is not exactly the same as edge. Might need to change this
                        for (String edge : event.getEdges()) {
                            spanBuilder.addLink(Util.toSpanContext(edge, false));
                        }
                        Span span = spanBuilder.startSpan();
                        Context context = span.storeInContext(Context.current());
                        context = context.with(AO_SDK_SPAN_ID_CONTEXT_KEY, span.getSpanContext().getSpanId());
                        Scope scope = context.makeCurrent();
                        scopeLookup.put(span, scope);
                    }
                },
                layer);
    }

    /**
     * {@inheritDoc}
     */
    public TraceEvent createExitEvent(String layer) {
        //to adhere to the original behavior, which metadata is captured when event is created
        Span span = Span.current();
        if (!validateSpanExit(span)) {
            return new NoOpEvent();
        } else {
            return new OpenTelemetryTraceEvent(
                    event -> {
                        Util.setSpanAttributes(span, event.getKeyValues());
                        span.end();
                        Scope scope = scopeLookup.remove(span);
                        if (scope != null) {
                            scope.close();
                        }
                    },
                    layer);
        }
    }

    /**
     * {@inheritDoc}
     */
    public TraceEvent createInfoEvent(String layer) {
        //to adhere to the original behavior, which metadata is captured when event is created
        Span span = Span.current();
        return new OpenTelemetryTraceEvent(
                event -> {
                    Util.setSpanAttributes(span, event.getKeyValues());
                },
                layer);
    }


    /**
     * {@inheritDoc}
     */
    public void logException(Throwable error) {
        Span span = Span.current();
        if (span.getSpanContext().isValid()) {
            span.recordException(error);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getCurrentXTraceID() {
        return Util.buildXTraceId(Span.current().getSpanContext());
    }
    
    @Override
    public String getCurrentLogTraceId() {
        return Util.buildMetadata(Span.current().getSpanContext()).getCompactTraceId();
    }

    private static final String TRACE_STATE_APPOPTICS_KEY = "appoptics";

    private TraceEvent startOrContinueTrace(String layer, String inXTraceID) {
        return new OpenTelemetryTraceEvent(
                event -> {
                    Scope remoteScope = null;
                    if (inXTraceID != null) {
                        Context extractedContext = GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                                .extract(Context.current(), Collections.singletonMap(TRACE_STATE_APPOPTICS_KEY, inXTraceID), new TextMapGetter<Map<String, String>>() {
                                    @Override
                                    public Iterable<String> keys(Map<String, String> carrier) {
                                        return new ArrayList<String>(carrier.keySet());
                                    }

                                    @Nullable
                                    @Override
                                    public String get(@Nullable Map<String, String> carrier, String key) {
                                        return carrier != null ? carrier.get(key) : null;
                                    }
                                });
                        remoteScope = extractedContext.makeCurrent();
                    }

                    SpanBuilder spanBuilder = tracer.spanBuilder(event.getOperationName());
                    Util.setSpanAttributes(spanBuilder, event.getKeyValues());
                    //TODO https://github.com/open-telemetry/opentelemetry-java/blob/main/QUICKSTART.md#create-spans-with-links, link is not exactly the same as edge. Might need to change this
                    for (String edge : event.getEdges()) {
                        spanBuilder.addLink(Util.toSpanContext(edge, false));
                    }
                    Span span = spanBuilder.startSpan();

                    Context context = span.storeInContext(Context.current());
                    context = context.with(AO_SDK_SPAN_ID_CONTEXT_KEY, span.getSpanContext().getSpanId());
                    //context = Baggage.current().toBuilder().put(AO_SDK_KEY, Boolean.toString(true)).build().storeInContext(context); //this does not work as all child span (even made by non sdk call, will have this key
                    scopeLookup.put(span, context.makeCurrent());
                    if (remoteScope != null) {
                        remoteScopeLookup.put(span, remoteScope);
                    }
                }, layer);
    }
    
    public boolean setTransactionName(String transactionName) {
        if (transactionName == null || "".equals(transactionName)) {
            return false;
        }
        if (!Span.current().getSpanContext().isValid()) {
            return false;
        }
//        long traceId = Util.toTraceId(Span.current().getSpanContext().getTraceIdBytes());
//        TracePropertyDictionary.getTracePropertiesByTraceId(traceId).put(com.tracelytics.joboe.span.impl.Span.TraceProperty.TRANSACTION_NAME, transactionName);

//        for (Method declaredMethod : RootSpan.class.getDeclaredMethods()) {
//            System.out.println(declaredMethod);
//        }
//        System.out.println(RootSpan.class.getProtectionDomain().getCodeSource().getLocation());
        Span rootSpan = RootSpan.fromContextOrNull(Context.current());
        if (rootSpan == null) {
            return false;
        }

        rootSpan.setAttribute("TransactionName", transactionName);

        return true;
    }
}
