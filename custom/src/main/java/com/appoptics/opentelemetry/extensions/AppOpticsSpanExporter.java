package com.appoptics.opentelemetry.extensions;

import com.tracelytics.joboe.*;
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
    private static final AttributeKey<Boolean> AO_SAMPLER_KEY = AttributeKey.booleanKey(com.appoptics.opentelemetry.extensions.Constants.AO_SAMPLER);

    private AppOpticsSpanExporter(String serviceKey) {

    }

    static Builder newBuilder(String serviceKey) {
        return new Builder(serviceKey);
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> collection) {
        for (SpanData spanData : collection) {
            if (spanData.hasEnded() && Boolean.TRUE == spanData.getAttributes().get(AO_SAMPLER_KEY)) {
                try {
                    Metadata parentMetadata = null;
                    if (spanData.getParentSpanContext().isValid()) {
                        parentMetadata = Util.buildMetadata(spanData.getParentSpanContext());
                    }

                    String entryXTraceId = Util.buildXTraceId(spanData.getSpanContext());

                    String spanName = spanData.getKind().toString() + "." + spanData.getName();

                    Metadata spanMetadata = new Metadata(entryXTraceId);
                    spanMetadata.randomizeOpID(); //get around the metadata logic, this op id is not used
                    Event entryEvent;
                    if (parentMetadata != null) {
                        entryEvent = new EventImpl(parentMetadata, entryXTraceId, true);
                    } else {
                        entryEvent = new EventImpl(null, entryXTraceId, false);
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
                    ;

                    Metadata exitMetadata = Util.buildSpanExitMetadata(spanData.getSpanContext()); //exit ID has to be generated
                    Event exitEvent = new EventImpl(spanMetadata, exitMetadata.toHexString(),true);
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

        return CompletableResultCode.ofSuccess();
    }

    private static final List<AttributeKey<?>> OPEN_TELEMETRY_ERROR_ATTRIBUTE_KEYS = Arrays.asList(
        SemanticAttributes.EXCEPTION_MESSAGE,
        SemanticAttributes.EXCEPTION_TYPE,
        SemanticAttributes.EXCEPTION_STACKTRACE
    );

    private void reportErrorEvent(EventData eventData) {
        Event event = Context.createEvent();
        Attributes attributes = eventData.getAttributes();
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

        Map<AttributeKey<?>, Object> otherKvs = filterAttributes(attributes);
        otherKvs.keySet().removeAll(OPEN_TELEMETRY_ERROR_ATTRIBUTE_KEYS);
        for (Map.Entry<AttributeKey<?>, Object> keyValue : otherKvs.entrySet()) {
            event.addInfo(keyValue.getKey().getKey(), keyValue.getValue());
        }

        event.setTimestamp(eventData.getEpochNanos() / 1000); //convert to micro second
        event.report();
    }

    private void reportInfoEvent(EventData eventData) {
        Event event = Context.createEvent();
        Attributes attributes = eventData.getAttributes();

        event.addInfo("Label", "info");

        Map<AttributeKey<?>, Object> otherKvs = filterAttributes(attributes);
        for (Map.Entry<AttributeKey<?>, Object> keyValue : otherKvs.entrySet()) {
            event.addInfo(keyValue.getKey().getKey(), keyValue.getValue());
        }

        event.setTimestamp(eventData.getEpochNanos() / 1000); //convert to micro second
        event.report();
    }

    private static Map<AttributeKey<?>, Object> filterAttributes(Attributes inputAttributes) {
        Map<AttributeKey<?>, Object> result = new HashMap<>();
        for (Map.Entry<AttributeKey<?>, Object> keyValue : inputAttributes.asMap().entrySet()) {
            AttributeKey<?> key = keyValue.getKey();
            if (!key.getKey().startsWith(com.appoptics.opentelemetry.extensions.Constants.AO_INTERNAL_ATTRIBUTE_PREFIX)) {
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
        Map<AttributeKey<?>, Object> attributes = filterAttributes(inputAttributes);
        Map<String, Object> tags = new HashMap<String, Object>();
        for (Map.Entry<AttributeKey<?>, Object> entry : attributes.entrySet()) {
            Object attributeValue = entry.getValue();
            String attributeKey = entry.getKey().getKey();
            if (ATTRIBUTE_TO_TAG.containsKey(attributeKey)) {
                String tagKey = ATTRIBUTE_TO_TAG.get(attributeKey);
                if (TAG_VALUE_TYPE.containsKey(tagKey)) {
                    attributeValue = TAG_VALUE_TYPE.get(tagKey).convert(attributeValue);
                }
                tags.put(tagKey, attributeValue);
            }

            //Add all attributes as KVs, but add/remove prefix based on type
            if (attributeKey.startsWith(com.appoptics.opentelemetry.extensions.Constants.AO_KEY_PREFIX)) {
                attributeKey = attributeKey.substring(com.appoptics.opentelemetry.extensions.Constants.AO_KEY_PREFIX.length());
            } else {
                attributeKey = Constants.OT_KEY_PREFIX + attributeKey;
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

//    private static Object getAttributeValue(AttributeValue attributeValue)   {
//        switch (attributeValue.getType()) {
//            case BOOLEAN:
//                return attributeValue.getBooleanValue();
//            case LONG:
//                return attributeValue.getLongValue();
//            case DOUBLE:
//                return attributeValue.getDoubleValue();
//            case STRING:
//                return attributeValue.getStringValue();
//            default:
//                System.err.println("Unknown type " + attributeValue.getType());
//        }
//        return null;
//    }



    @Override
    public void close() {

    }

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
