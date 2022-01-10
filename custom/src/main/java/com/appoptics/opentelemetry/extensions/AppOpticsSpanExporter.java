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

    private AppOpticsSpanExporter(String serviceKey) {
    }

    static Builder newBuilder(String serviceKey) {
        return new Builder(serviceKey);
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

                    final String spanName = spanData.getKind().toString() + "." + spanData.getName();

                    final Metadata spanMetadata = new Metadata(w3cContext);
                    spanMetadata.randomizeOpID(); //get around the metadata logic, this op id is not used
                    Event entryEvent;
                    if (parentMetadata != null) {
                        entryEvent = new EventImpl(parentMetadata, w3cContext, true);
                    } else {
                        entryEvent = new EventImpl(null, w3cContext, false);
                    }

                    if (!spanData.getParentSpanContext().isValid() || spanData.getParentSpanContext().isRemote()) { //then a root span of this service
                        String transactionName = spanData.getAttributes().get(AttributeKey.stringKey("TransactionName")); //check if there's transaction name set as attribute
                        if (transactionName == null) {
                            transactionName = TransactionNameManager.getTransactionName(spanData);
                            if (transactionName != null) {
                                entryEvent.addInfo("TransactionName", transactionName); //only do this if we are generating a transaction name here. If it's already in attributes, it will be inserted by addInfo(getTags...)
                            }
                        }

                    }

                    entryEvent.addInfo(
                            "Label", "entry",
                            "Layer", spanName);
                    entryEvent.setTimestamp(spanData.getStartEpochNanos() / 1000);
                    entryEvent.addInfo(getEventKvs(spanData.getAttributes()));
                    entryEvent.report(spanMetadata);

                    for (EventData event : spanData.getEvents()) {
                        if (SemanticAttributes.EXCEPTION_EVENT_NAME.equals(event.getName())) {
                            reportErrorEvent(event);
                        } else {
                            reportInfoEvent(event);
                        }
                    }

                    final Metadata exitMetadata = Util.buildSpanExitMetadata(spanData.getSpanContext()); //exit ID has to be generated
                    final Event exitEvent = new EventImpl(spanMetadata, exitMetadata.toHexString(),true);
                    exitEvent.addInfo(
                            "Label", "exit",
                            "Layer", spanName);

                    exitEvent.setTimestamp(spanData.getEndEpochNanos() / 1000);
                    exitEvent.report(spanMetadata);
                } catch (OboeException e) {
                    e.printStackTrace();
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

        event.addInfo("Label", "info");

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




    private static final Map<String, String> ATTRIBUTE_TO_TAG = new HashMap<String, String>();
    private static final Map<String, TypeConverter<?>> TAG_VALUE_TYPE = new HashMap<String, TypeConverter<?>>();
    static {
        ATTRIBUTE_TO_TAG.put("http.status_code", "Status");
        ATTRIBUTE_TO_TAG.put("net.peer.ip", "ClientIP");
        ATTRIBUTE_TO_TAG.put("http.url", "URL");
        ATTRIBUTE_TO_TAG.put("http.method", "HTTPMethod");
        ATTRIBUTE_TO_TAG.put("db.connection_string", "RemoteHost");
        ATTRIBUTE_TO_TAG.put("db.system", "Flavor");
        ATTRIBUTE_TO_TAG.put("db.statement", "Query");
        ATTRIBUTE_TO_TAG.put("db.url", "RemoteHost");
        TAG_VALUE_TYPE.put("Status", IntConverter.INSTANCE);
    }

    private Map<String,?> getEventKvs(Attributes inputAttributes) {
        final Map<AttributeKey<?>, Object> attributes = filterAttributes(inputAttributes);
        final Map<String, Object> tags = new HashMap<String, Object>();
        for (Map.Entry<AttributeKey<?>, Object> entry : attributes.entrySet()) {
            Object attributeValue = entry.getValue();
            final String attributeKey = entry.getKey().getKey();
            if (ATTRIBUTE_TO_TAG.containsKey(attributeKey)) {
                final String tagKey = ATTRIBUTE_TO_TAG.get(attributeKey);
                if (TAG_VALUE_TYPE.containsKey(tagKey)) {
                    attributeValue = TAG_VALUE_TYPE.get(tagKey).convert(attributeValue);
                }
                tags.put(tagKey, attributeValue);
            }

            tags.put(attributeKey, attributeValue);
        }
        return tags;
    }

    interface TypeConverter<T> {
        T convert(Object rawValue);
    }

    private static class IntConverter implements TypeConverter<Integer> {
        static final IntConverter INSTANCE = new IntConverter();
        @Override
        public Integer convert(Object rawValue) {
            if (rawValue instanceof Number) {
                return ((Number) rawValue).intValue();
            } else if (rawValue instanceof String) {
                return Integer.valueOf((String) rawValue);
            } else {
                return null;
            }
        }
    }

    @Override
    public void close() { }

    public static class Builder {

        private final String serviceKey;

        public Builder(String serviceKey) {
            this.serviceKey = serviceKey;
        }

        AppOpticsSpanExporter build() {
            return new AppOpticsSpanExporter(serviceKey);
        }
    }
}
