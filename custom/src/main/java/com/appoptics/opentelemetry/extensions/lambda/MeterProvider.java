package com.appoptics.opentelemetry.extensions.lambda;

import com.appoptics.opentelemetry.extensions.initialize.config.BuildConfig;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.Meter;

public class MeterProvider {

    public static final String samplingMeterName = "sw.apm.sampling.metrics";

    public static final String requestMeterName = "sw.apm.request.metrics";

    public static Meter getSamplingMetricsMeter(){
        return GlobalOpenTelemetry.meterBuilder(samplingMeterName)
                .setInstrumentationVersion(BuildConfig.SOLARWINDS_AGENT_VERSION)
                .build();
    }

    public static Meter getRequestMetricsMeter(){
        return GlobalOpenTelemetry.meterBuilder(requestMeterName)
                .setInstrumentationVersion(BuildConfig.SOLARWINDS_AGENT_VERSION)
                .build();
    }
}
