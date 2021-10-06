package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.core.Util;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.RpcEventReporter;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.ProfilerSetting;
import com.tracelytics.joboe.rpc.RpcClientManager;
import com.tracelytics.profiler.Profiler;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

import static com.appoptics.opentelemetry.core.Constants.*;

/**
 * Span process to perform code profiling
 */
public class AppOpticsProfilingSpanProcessor implements SpanProcessor {
    private static ProfilerSetting profilerSetting = (ProfilerSetting) ConfigManager.getConfig(ConfigProperty.PROFILER);
    private static final boolean PROFILER_ENABLED = profilerSetting != null && profilerSetting.isEnabled();
    static {
        Profiler.initialize(profilerSetting, RpcEventReporter.buildReporter(RpcClientManager.OperationType.PROFILING));
    }


    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        if (span.getSpanContext().isSampled()) { //only profile on sampled spans
            SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
            if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) { //then a root span of this service
                if (PROFILER_ENABLED) {
                    Metadata metadata = Util.buildMetadata(span.getSpanContext());
                    Profiler.addProfiledThread(Thread.currentThread(), metadata, metadata.getTraceId());
                    span.setAttribute(SW_KEY_PREFIX + "ProfileSpans", 1);
                } else {
                    span.setAttribute(SW_KEY_PREFIX + "ProfileSpans", -1); //profiler disabled
                }
            }
        }
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        if (span.getSpanContext().isSampled()) { //only profile on sampled spans
            SpanContext parentSpanContext = span.toSpanData().getParentSpanContext();
            if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) { //then a root span of this service
                Profiler.stopProfile(Util.toTraceId(span.getSpanContext().getTraceIdBytes()));
            }
        }
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }
}
