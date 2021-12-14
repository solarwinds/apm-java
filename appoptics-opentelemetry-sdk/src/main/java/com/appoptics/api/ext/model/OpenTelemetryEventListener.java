package com.appoptics.api.ext.model;


public interface OpenTelemetryEventListener {
    void onReport(OpenTelemetryTraceEvent event);
}
