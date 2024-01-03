package com.appoptics.opentelemetry.extensions;

import static com.appoptics.opentelemetry.core.Constants.SW_KEY_PREFIX;

import com.appoptics.opentelemetry.core.Util;
import com.solarwinds.joboe.core.Metadata;
import com.solarwinds.joboe.core.RpcEventReporter;
import com.solarwinds.joboe.core.config.ConfigManager;
import com.solarwinds.joboe.core.config.ConfigProperty;
import com.solarwinds.joboe.core.config.ProfilerSetting;
import com.solarwinds.joboe.core.logging.Logger;
import com.solarwinds.joboe.core.logging.LoggerFactory;
import com.solarwinds.joboe.core.profiler.Profiler;
import com.solarwinds.joboe.core.rpc.RpcClientManager;
import com.solarwinds.joboe.shaded.javax.annotation.Nonnull;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

/** Span process to perform code profiling */
public class AppOpticsProfilingSpanProcessor implements SpanProcessor {
  private static final Logger logger = LoggerFactory.getLogger();
  private static final ProfilerSetting profilerSetting =
      (ProfilerSetting) ConfigManager.getConfig(ConfigProperty.PROFILER);
  private static final boolean PROFILER_ENABLED =
      profilerSetting != null && profilerSetting.isEnabled();

  static {
    if (PROFILER_ENABLED) {
      Profiler.initialize(
          profilerSetting,
          RpcEventReporter.buildReporter(RpcClientManager.OperationType.PROFILING));
    } else {
      logger.info("Profiler is disabled.");
    }
  }

  @Override
  public void onStart(@Nonnull Context parentContext, ReadWriteSpan span) {
    if (span.getSpanContext().isSampled()) { // only profile on sampled spans
      SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
      if (!parentSpanContext.isValid()
          || parentSpanContext.isRemote()) { // then a root span of this service
        if (PROFILER_ENABLED) {
          SpanContext spanContext = span.getSpanContext();
          Metadata metadata = Util.buildMetadata(spanContext);
          if (metadata.isValid()) {
            Profiler.addProfiledThread(
                Thread.currentThread(), metadata, Metadata.bytesToHex(metadata.getTaskID()));
            span.setAttribute(SW_KEY_PREFIX + "ProfileSpans", 1);
          }
        } else {
          span.setAttribute(SW_KEY_PREFIX + "ProfileSpans", -1); // profiler disabled
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
    if (span.getSpanContext().isSampled() && PROFILER_ENABLED) { // only profile on sampled spans
      SpanContext parentSpanContext = span.toSpanData().getParentSpanContext();
      if (!parentSpanContext.isValid()
          || parentSpanContext.isRemote()) { // then a root span of this service
        Profiler.stopProfile(span.getSpanContext().getTraceId());
      }
    }
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }
}
