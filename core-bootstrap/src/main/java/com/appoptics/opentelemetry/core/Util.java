package com.appoptics.opentelemetry.core;

import com.tracelytics.ext.google.common.base.Strings;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.OboeException;
import com.tracelytics.joboe.Constants;
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
    private static Logger logger = LoggerFactory.getLogger(Util.class.getName());
    private static String APPOPTICS_TRACE_STATE_KEY = "appoptics";
    private static byte EXIT_OP_ID_MASK = 0xf;
    public static String buildXTraceId(SpanContext context) {
        String aoId = context.getTraceState().get(APPOPTICS_TRACE_STATE_KEY);

        String traceId = context.getTraceId();
        if (aoId != null) {
            try {
                traceId = new Metadata(aoId).taskHexString();
            } catch (OboeException e) {
                logger.warn("Failed to convert appoptics trace state [" + aoId + "] to OT trace id", e);
            }
        }
        return buildXTraceId(traceId, context.getSpanId(), context.isSampled());
    }

    public static Metadata buildSpanExitMetadata(SpanContext context) {
        Metadata entryContext = buildMetadata(context);
        byte[] exitOpID = Arrays.copyOf(entryContext.getOpID(), entryContext.getOpID().length);
        exitOpID[exitOpID.length - 1] = (byte)(exitOpID[exitOpID.length - 1] ^ EXIT_OP_ID_MASK); //flip the last byte
        Metadata exitContext = new Metadata(entryContext);
        exitContext.setOpID(exitOpID);
        return exitContext;
    }

    public static String buildXTraceId(String traceId, String spanId, boolean isSampled) {
        final String HEADER = "2B";
        String hexString = HEADER +
                Strings.padEnd(traceId, Constants.MAX_TASK_ID_LEN * 2, '0') +
                Strings.padEnd(spanId, Constants.MAX_OP_ID_LEN * 2, '0');
        hexString += isSampled ? "01" : "00";


        return hexString.toUpperCase();
    }

    public static Metadata buildMetadata(SpanContext context) {
        try {
            Metadata metadata = new Metadata(buildXTraceId(context));
            metadata.setTraceId(toTraceId(context.getTraceIdBytes()));
            return metadata;
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

    public static SpanContext toSpanContext(String xTrace, boolean isRemote) {
        W3TraceContextHolder w3TraceContext = toW3TraceContext(xTrace);
        return isRemote
                ? SpanContext.createFromRemoteParent(w3TraceContext.traceId, w3TraceContext.spanId, w3TraceContext.traceFlags, TraceState.getDefault())
                : SpanContext.create(w3TraceContext.traceId, w3TraceContext.spanId, w3TraceContext.traceFlags, TraceState.getDefault());
    }

    public static W3TraceContextHolder toW3TraceContext(String xTrace) {
        Metadata metadata;
        try {
            metadata = new Metadata(xTrace);
        } catch (OboeException e) {
            e.printStackTrace();
            return null;
        }

        String w3TraceId = TraceId.fromBytes(metadata.getTaskID());
        String w3SpanId = SpanId.fromBytes(metadata.getOpID());
        TraceFlags w3TraceFlags = metadata.isSampled() ? TraceFlags.getSampled() : TraceFlags.getDefault();

        return new W3TraceContextHolder(w3TraceId, w3SpanId, w3TraceFlags);
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


    public static class W3TraceContextHolder {
        public final String traceId;
        public final String spanId;
        public final TraceFlags traceFlags;

        W3TraceContextHolder(String traceId, String spanId, TraceFlags traceFlags) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.traceFlags = traceFlags;
        }
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

    public static void setSpanAttributes(Span span, Map<String, ?> attributes) {
        for (Map.Entry<String, ?> entry : attributes.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();
            if (!key.startsWith(AO_KEY_PREFIX)) {
                key = AO_KEY_PREFIX + key;
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

    public static void setSpanAttributes(SpanBuilder spanBuilder, Map<String, ?> attributes) {
        for (Map.Entry<String, ?> entry : attributes.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();
            if (!key.startsWith(AO_KEY_PREFIX)) {
                key = AO_KEY_PREFIX + key;
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
