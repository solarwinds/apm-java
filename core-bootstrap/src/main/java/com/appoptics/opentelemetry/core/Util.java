package com.appoptics.opentelemetry.core;

import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.OboeException;
import io.opentelemetry.api.trace.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.appoptics.opentelemetry.core.Constants.*;

public class Util {
    private static final Logger logger = LoggerFactory.getLogger(Util.class.getName());
    private static final byte EXIT_OP_ID_MASK = 0xf;

    /** Converts an OpenTelemetry span context to a hex string.
     *
     * @param context
     * @return
     */
    public static String W3CContextToHexString(SpanContext context) {
        return Metadata.CURRENT_VERSION_HEXSTRING
                + Metadata.HEXSTRING_DELIMETER
                + context.getTraceId()
                + Metadata.HEXSTRING_DELIMETER
                + context.getSpanId()
                + Metadata.HEXSTRING_DELIMETER
                + context.getTraceFlags().asHex();
    }

    /**
     * Generates the AO metadata for the "exit event" of an OT span.
     *
     * Since OT has no concept of "exit event", we will need to generate the AO op-id for the metadata of AO exit event
     *
     * @param context
     * @return
     */
    public static Metadata buildSpanExitMetadata(SpanContext context) {
        Metadata entryContext = buildMetadata(context);
        byte[] exitOpID = Arrays.copyOf(entryContext.getOpID(), entryContext.getOpID().length);
        exitOpID[exitOpID.length - 1] = (byte)(exitOpID[exitOpID.length - 1] ^ EXIT_OP_ID_MASK); //flip the last byte
        Metadata exitContext = new Metadata(entryContext);
        exitContext.setOpID(exitOpID);
        return exitContext;
    }

    /**
     * Builds an AO metadata with OT span context
     *
     * @param context
     * @return
     */
    public static Metadata buildMetadata(SpanContext context) {
        try {
            return new Metadata(W3CContextToHexString(context));
        } catch (OboeException e) {
            return null;
        }
    }

    /**
     * Generate a deterministic AO trace id from the OT trace id bytes
     * @param traceIdBytes
     * @return
     */
    public static Long toTraceId(byte[] traceIdBytes) {
        long value = 0;
        int length = Math.min(8, traceIdBytes.length);
        for (int i = 0; i < length; i++) {
            value += ((long) traceIdBytes[i] & 0xffL) << (8 * i);
        }
        return value;
    }

    /**
     * Builds an OT span context with AO x-trace ID
     * @param xTrace
     * @param isRemote
     * @return
     */
    public static SpanContext toSpanContext(String xTrace, boolean isRemote) {
        Metadata metadata = null;
        try {
            metadata = new Metadata(xTrace);
        } catch (OboeException e) {
            return SpanContext.getInvalid();
        }

        return isRemote
                ? SpanContext.createFromRemoteParent(metadata.taskHexString(), metadata.opHexString(), TraceFlags.fromByte(metadata.getFlags()), TraceState.getDefault())
                : SpanContext.create(metadata.taskHexString(), metadata.opHexString(), TraceFlags.fromByte(metadata.getFlags()), TraceState.getDefault());
    }

    public static String parsePath(String url) {
        if (url != null) {
            try {
                return new URL(url).getPath();
            } catch (MalformedURLException e) {
                logger.debug("Cannot parse URL " + url);
            }
        }
        return null;
    }

    public static Map<String, Object> keyValuePairsToMap(Object... keyValuePairs) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (keyValuePairs.length % 2 == 1) {
            logger.warn("Expect even number of arugments but found " + keyValuePairs.length + " arguments");
            return map;
        }

        for(int i = 0; i < keyValuePairs.length / 2; i++) {
            if (!(keyValuePairs[i * 2] instanceof String)) {
                logger.warn("Expect String argument at position " + (i * 2 + 1) + " but found " + keyValuePairs[i * 2]);
                continue;
            }
            map.put((String) keyValuePairs[i * 2], keyValuePairs[i * 2 + 1]);
        }

        return map;
    }

    /**
     * A convenient method to set span attributes with a Map with values of any types.
     *
     * Take note that if the type is not supported, its `toString` value will be used.
     *
     * @param span
     * @param attributes
     */
    public static void setSpanAttributes(Span span, Map<String, ?> attributes) {
        for (Map.Entry<String, ?> entry : attributes.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();
            if (!key.startsWith(SW_KEY_PREFIX)) {
                key = SW_KEY_PREFIX + key;
            }
            if (value instanceof String) {
                span.setAttribute(key, (String) value);
            } else if (value instanceof Double) {
                span.setAttribute(key, (Double) value);
            } else if (value instanceof Boolean) {
                span.setAttribute(key, (Boolean) value);
            } else if (value instanceof Long) {
                span.setAttribute(key, (Long) value);
            } else {
                if (value != null) {
                    span.setAttribute(key, value.toString());
                }
            }
        }
    }

    /**
     * A convenient method to set span builder attributes with a Map with values of any types.
     *
     * Take note that if the type is not supported, its `toString` value will be used.
     *
     * @param spanBuilder
     * @param attributes
     */
    public static void setSpanAttributes(SpanBuilder spanBuilder, Map<String, ?> attributes) {
        for (Map.Entry<String, ?> entry : attributes.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();
            if (!key.startsWith(SW_KEY_PREFIX)) {
                key = SW_KEY_PREFIX + key;
            }

            if (value instanceof String) {
                spanBuilder.setAttribute(key, (String) value);
            } else if (value instanceof Double) {
                spanBuilder.setAttribute(key, (Double) value);
            } else if (value instanceof Boolean) {
                spanBuilder.setAttribute(key, (Boolean) value);
            } else if (value instanceof Long) {
                spanBuilder.setAttribute(key, (Long) value);
            } else {
                if (value != null) {
                    spanBuilder.setAttribute(key, value.toString());
                }
            }
        }
    }
}
