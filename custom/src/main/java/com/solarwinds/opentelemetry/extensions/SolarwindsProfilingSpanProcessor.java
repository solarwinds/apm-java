/*
 * Copyright SolarWinds Worldwide, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solarwinds.opentelemetry.extensions;

import static com.solarwinds.opentelemetry.core.Constants.SW_KEY_PREFIX;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.core.ReporterFactory;
import com.solarwinds.joboe.core.profiler.Profiler;
import com.solarwinds.joboe.core.profiler.ProfilerSetting;
import com.solarwinds.joboe.core.rpc.ClientException;
import com.solarwinds.joboe.core.rpc.RpcClientManager;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.Metadata;
import com.solarwinds.joboe.shaded.javax.annotation.Nonnull;
import com.solarwinds.opentelemetry.core.Util;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

/** Span process to perform code profiling */
public class SolarwindsProfilingSpanProcessor implements SpanProcessor {
  private static final Logger logger = LoggerFactory.getLogger();
  private static final ProfilerSetting profilerSetting =
      (ProfilerSetting) ConfigManager.getConfig(ConfigProperty.PROFILER);
  private static final boolean PROFILER_ENABLED =
      profilerSetting != null && profilerSetting.isEnabled();

  static {
    if (PROFILER_ENABLED) {
      try {
        Profiler.initialize(
            profilerSetting,
            ReporterFactory.getInstance()
                .createQueuingEventReporter(
                    RpcClientManager.getClient(RpcClientManager.OperationType.PROFILING)));
      } catch (ClientException e) {
        logger.error("Error creating profiling report", e);
        throw new RuntimeException(e);
      }
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
