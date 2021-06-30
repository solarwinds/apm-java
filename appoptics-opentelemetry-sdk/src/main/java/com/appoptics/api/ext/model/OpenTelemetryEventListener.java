package com.appoptics.api.ext.model;

import com.appoptics.api.ext.TraceEvent;

public interface OpenTelemetryEventListener {
    void onReport(OpenTelemetryTraceEvent event);
}
