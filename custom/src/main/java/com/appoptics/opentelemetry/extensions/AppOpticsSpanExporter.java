package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.core.Util;
import com.tracelytics.joboe.*;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import java.util.*;

/**
 * Span exporter to be used with the OpenTelemetry auto agent
 */
public class AppOpticsSpanExporter implements SpanExporter {
    private final Logger logger = LoggerFactory.getLogger();

    // This format is visible to customer via span layer and can be used to configure transaction filtering setting.
    static final String LAYER_FORMAT = "%s:%s";

    private AppOpticsSpanExporter() {
    }

    static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> collection) {
        logger.debug("Started to export span data to the collector.");
        for (SpanData spanData : collection) {
            if (spanData.hasEnded()) {
                try {
                    Metadata parentMetadata = null;
                    if (spanData.getParentSpanContext().isValid()) {
                        parentMetadata = Util.buildMetadata(spanData.getParentSpanContext());
                    }

                    final String w3cContext = Util.w3CContextToHexString(spanData.getSpanContext());
                    final String spanName = String.format(LAYER_FORMAT, spanData.getKind(), spanData.getName());

                    final Metadata spanMetadata = new Metadata(w3cContext);
                    spanMetadata.randomizeOpID(); //get around the metadata logic, this op id is not used
                    // set spanMetadata into the current Context so that after the initial event, the metadata
                    // does not need to be manually managed for event creation / linkage.  Context is updated
                    // with the metadata of the last reported event.
                    Context.setMetadata(spanMetadata);

                    // create new event with an override metadata from the OTel spanData (w3cContext)
                    // and link to OTel parent (if present).
                    // do this with EventImpl instead of Context because we need to use the addEdge param.
                    Event entryEvent;
                    if (parentMetadata != null) {
                        entryEvent = new EventImpl(parentMetadata, w3cContext, true);
                    } else {
                        entryEvent = new EventImpl(null, w3cContext, false);
                    }

                    if (!spanData.getParentSpanContext().isValid() || spanData.getParentSpanContext().isRemote()) { //then a root span of this service
                        String transactionName = spanData.getAttributes().get(AttributeKey.stringKey(
                                "TransactionName")); //check if there's transaction name set as attribute
                        if (transactionName == null) {
                            transactionName = TransactionNameManager.getTransactionName(spanData);
                            if (transactionName != null) {
                                entryEvent.addInfo("TransactionName",
                                        transactionName); //only do this if we are generating a transaction name here. If it's already in attributes, it will be inserted by addInfo(getTags...)
                            }
                        }

                    }

                    entryEvent.addInfo(
                            "Label", "entry",
                            "Layer", spanName,
                            "sw.span_kind", spanData.getKind().toString());
                    entryEvent.setTimestamp(spanData.getStartEpochNanos() / 1000);
                    entryEvent.addInfo(getEventKvs(spanData.getAttributes()));
                    entryEvent.report();

                    for (EventData event : spanData.getEvents()) {
                        if (SemanticAttributes.EXCEPTION_EVENT_NAME.equals(event.getName())) {
                            reportErrorEvent(event);
                        } else {
                            reportInfoEvent(event);
                        }
                    }

                    final Event exitEvent = Context.createEvent();
                    exitEvent.addInfo(
                            "Label", "exit",
                            "Layer", spanName,
                            "sw.span_status_code", spanData.getStatus().getStatusCode().toString(),
                            "sw.span_status_message", spanData.getStatus().getDescription());
                    exitEvent.setTimestamp(spanData.getEndEpochNanos() / 1000);
                    exitEvent.report();
                } catch (OboeException e) {
                    e.printStackTrace();
                } finally {
                    // clear Context for the next OTel span, which will initialize it with w3cContext
                    Context.clearMetadata();
                }
            }
        }

        logger.debug("Finished sending " + collection.size() + " spans to the collector.");
        return CompletableResultCode.ofSuccess();
    }

    private static final List<AttributeKey<?>> OPEN_TELEMETRY_ERROR_ATTRIBUTE_KEYS = Arrays.asList(
            SemanticAttributes.EXCEPTION_MESSAGE,
            SemanticAttributes.EXCEPTION_TYPE,
            SemanticAttributes.EXCEPTION_STACKTRACE
    );

    private void reportErrorEvent(EventData eventData) {
        final Event event = Context.createEvent();
        final Attributes attributes = eventData.getAttributes();
        String message = attributes.get(SemanticAttributes.EXCEPTION_MESSAGE);
        if (message == null) {
            message = "";
        }
        event.addInfo("Label", "error",
                "Spec", "error",
                "ErrorClass", attributes.get(SemanticAttributes.EXCEPTION_TYPE),
                "ErrorMsg", message,
                "Backtrace", attributes.get(SemanticAttributes.EXCEPTION_STACKTRACE)
        );

        final Map<AttributeKey<?>, Object> otherKvs = filterAttributes(attributes);
        otherKvs.keySet().removeAll(OPEN_TELEMETRY_ERROR_ATTRIBUTE_KEYS);
        for (Map.Entry<AttributeKey<?>, Object> keyValue : otherKvs.entrySet()) {
            event.addInfo(keyValue.getKey().getKey(), keyValue.getValue());
        }
        event.setTimestamp(eventData.getEpochNanos() / 1000); //convert to micro second
        event.report();
    }

    private void reportInfoEvent(EventData eventData) {
        final Event event = Context.createEvent();
        final Attributes attributes = eventData.getAttributes();
        event.addInfo(
                "Label", "info",
                "sw.event_name", eventData.getName());
        final Map<AttributeKey<?>, Object> otherKvs = filterAttributes(attributes);
        for (Map.Entry<AttributeKey<?>, Object> keyValue : otherKvs.entrySet()) {
            event.addInfo(keyValue.getKey().getKey(), keyValue.getValue());
        }

        event.setTimestamp(eventData.getEpochNanos() / 1000); //convert to micro second
        event.report();
    }

    private static Map<AttributeKey<?>, Object> filterAttributes(Attributes inputAttributes) {
        final Map<AttributeKey<?>, Object> result = new HashMap<>();
        for (Map.Entry<AttributeKey<?>, Object> keyValue : inputAttributes.asMap().entrySet()) {
            AttributeKey<?> key = keyValue.getKey();
            if (!key.getKey().startsWith(com.appoptics.opentelemetry.core.Constants.SW_INTERNAL_ATTRIBUTE_PREFIX)) {
                result.put(key, keyValue.getValue());
            }
        }
        return result;
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }

    private Map<String, ?> getEventKvs(Attributes inputAttributes) {
        final Map<AttributeKey<?>, Object> attributes = filterAttributes(inputAttributes);
        final Map<String, Object> tags = new HashMap<String, Object>();
        for (Map.Entry<AttributeKey<?>, Object> entry : attributes.entrySet()) {
            Object attributeValue = entry.getValue();
            final String attributeKey = entry.getKey().getKey();

            tags.put(attributeKey, attributeValue);
        }
        return tags;
    }

    @Override
    public void close() {
    }

    public static class Builder {

        public Builder() {
        }

        AppOpticsSpanExporter build() {
            return new AppOpticsSpanExporter();
        }
    }
}
