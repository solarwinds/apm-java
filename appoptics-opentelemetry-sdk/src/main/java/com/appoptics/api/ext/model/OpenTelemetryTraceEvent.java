package com.appoptics.api.ext.model;

import com.appoptics.api.ext.TraceEvent;
import com.tracelytics.util.BackTraceUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class OpenTelemetryTraceEvent implements TraceEvent {
    private final OpenTelemetryEventListener listener;
    private final String operationName;
    private final Map<String, Object> kvs = new ConcurrentHashMap<String, Object>();
    private final List<String> edges = new ArrayList<String>();
    private Logger logger = Logger.getLogger(OpenTelemetryTraceEvent.class.getName());

    public OpenTelemetryTraceEvent(OpenTelemetryEventListener listener, String operationName) {
        this.listener = listener;
        this.operationName = operationName;
    }

    @Override
    public final void report() {
        listener.onReport(this);
    }

    public String getOperationName() {
        return operationName;
    }

    public void addInfo(String key, Object value) {
        kvs.put(key, value);
    }

    public void addInfo(Map<String, Object> infoMap) {
        kvs.putAll(infoMap);
    }

    public void addInfo(Object... info) {
        if (info.length % 2 != 0) {
            logger.warning("Expect even number of arguments for addInfo(Object...) call but found " + info.length);
        }
        for (int i = 0 ; i < info.length / 2; i ++) {
            int keyIndex = i * 2;
            int valueIndex = keyIndex + 1;
            if (!(info[keyIndex] instanceof String)) {
                logger.warning("Expect String argument for odd n-th argument for addInfo(Object...) call but found " + (info[keyIndex] != null ? info[keyIndex].getClass().getName() : "null"));
                addInfo((String) info[keyIndex], info[valueIndex]);
            }
        }
    }

    public void setAsync() {
        kvs.put(com.tracelytics.joboe.Constants.XTR_ASYNC_KEY, true);
    }

    public void addEdge(String xTraceID) {
        edges.add(xTraceID);
    }

    public void addBackTrace() {
        kvs.put("Backtrace", BackTraceUtil.backTraceToString(BackTraceUtil.getBackTrace(1)));
    }

    public Map<String, Object> getKeyValues() {
        return kvs;
    }

    public List<String> getEdges() {
        return edges;
    }
}
