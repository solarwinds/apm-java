package com.appoptics.api.ext.model;

import java.util.Map;

import com.appoptics.api.ext.TraceEvent;

public class NoOpEvent implements TraceEvent {
    public void addInfo(String key, Object value) { /* NoOp */ }
    public void addInfo(Map<String, Object> infoMap) { /* NoOp */ }
    public void addInfo(Object... info) { /* NoOp */ }
    public void setAsync() { /* NoOp */ }
    public void addEdge(String xTraceID) { /* NoOp */ }
    public void report() { /*NoOp */ }
    public void addBackTrace() { /*NoOp */ }
}
