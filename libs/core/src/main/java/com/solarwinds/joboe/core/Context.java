/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
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

package com.solarwinds.joboe.core;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.Metadata;
import com.solarwinds.joboe.sampling.SamplingException;
import com.solarwinds.joboe.sampling.SettingsArg;
import com.solarwinds.joboe.sampling.SettingsArgChangeListener;
import com.solarwinds.joboe.sampling.SettingsManager;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import java.util.Arrays;

public class Context {
  private static final ThreadLocal<Boolean> skipInheritingContextThreadLocal =
      new ThreadLocal<Boolean>();
  private static final Logger logger = LoggerFactory.getLogger();
  private static boolean inheritContext =
      true; // whether child thread should inherits metadata (clone) from parent

  // Thread local storage for metadata. This is inheritable so that child threads pick up the
  // parent's context.
  private static final InheritableThreadLocal<Metadata> mdThreadLocal =
      new InheritableThreadLocal<Metadata>() {
        @Override
        protected Metadata initialValue() {
          Metadata md = new Metadata();
          return md;
        }

        @Override
        protected Metadata childValue(Metadata parentMetadata) {
          if (!inheritContext
              || (skipInheritingContextThreadLocal.get() != null
                  && skipInheritingContextThreadLocal.get())) {
            return new Metadata(); // do not propagate context here, return an empty context
          } else {
            Metadata clonedMetadata = new Metadata(parentMetadata);
            // if parent span is sampled, that means a parent span exists then this is a child span
            // spawn off from a thread from parent span, mark this as asynchronous
            if (parentMetadata.isSampled()) {
              clonedMetadata.setIsAsync(true);
            }
            return clonedMetadata;
          }
        }
      };

  static {
    SettingsManager.registerListener(
        new SettingsArgChangeListener<Boolean>(
            SettingsArg
                .DISABLE_INHERIT_CONTEXT) { // listen to Settings change on inheriting context
          @Override
          public void onChange(Boolean newValue) {
            if (newValue != null) {
              inheritContext = !newValue;
            } else {
              inheritContext = true; // by default we inherit context
            }
          }
        });
  }

  public static Event createEvent() {
    return createEventWithContext(getMetadata(), true);
  }

  public static Event createEventWithID(String metadataID) throws SamplingException {
    return createEventWithIDAndContext(metadataID, getMetadata());
  }

  public static Event createEventWithContext(Metadata context) {
    return createEventWithContext(context, true); // by default add edge (not trace start)
  }

  public static Event createEventWithContext(Metadata context, boolean addEdge) {
    if (shouldCreateEvent(context)) {
      return new EventImpl(context, addEdge);
    } else {
      return new NoopEvent(context);
    }
  }

  public static Event createEventWithIDAndContext(String metadataID, Metadata currentContext)
      throws SamplingException {
    if (shouldCreateEvent(currentContext)) {
      return new EventImpl(currentContext, metadataID, true);
    } else {
      return new NoopEvent(currentContext);
    }
  }

  public static Event createEventWithGeneratedMetadata(Metadata generatedMetadata) {
    return createEventWithGeneratedMetadata(null, generatedMetadata);
  }

  public static Event createEventWithGeneratedMetadata(
      Metadata parentMetadata, Metadata generatedMetadata) {
    if (shouldCreateEvent(generatedMetadata)) {
      return new EventImpl(parentMetadata, generatedMetadata);
    } else {
      return new NoopEvent(generatedMetadata);
    }
  }

  private static boolean shouldCreateEvent(Metadata context) {
    if (!context.isSampled()) {
      return false;
    }

    if (!context.incrNumEvents()) {
      // Should not invalidate as metrics should still be captured, setting it to not sampled might
      // also impact logic that assume a "sampled" entry point should match a "sampled" exit point
      return false;
    }

    if (context.isExpired(System.currentTimeMillis())) {
      // Consider an error condition (context leaking) Hence we should expire the context metadata
      // to stop further processing/leaking
      context.invalidate();
      return false;
    }

    return true;
  }

  /** Returns metadata for current thread */
  public static Metadata getMetadata() {
    Metadata md = mdThreadLocal.get();
    if (Arrays.equals(md.getTaskID(), Metadata.unsetTaskID)) {
      SpanContext sc = Span.current().getSpanContext();
      if (sc.isValid()) {
        return new Metadata(sc);
      }
    }
    return md;
  }

  /**
   * Sets metadata for current thread.
   *
   * <p><b>Note:</b> This sets the metadata in the local thread storage only. It does NOT update the
   * OpenTelemetry context. To propagate context via OpenTelemetry, please use {@code
   * io.opentelemetry.context.Context.makeCurrent()}.
   *
   * @param md
   * @deprecated Use OpenTelemetry Context propagation instead.
   */
  @Deprecated
  public static void setMetadata(Metadata md) {
    setMap(md);
  }

  /**
   * Sets metadata for this thread from hex string
   *
   * <p><b>Note:</b> This sets the metadata in the local thread storage only. It does NOT update the
   * OpenTelemetry context. To propagate context via OpenTelemetry, please use {@code
   * io.opentelemetry.context.Context.makeCurrent()}.
   *
   * @param hexStr
   * @throws SamplingException
   * @deprecated Use OpenTelemetry Context propagation instead.
   */
  @Deprecated
  public static void setMetadata(String hexStr) throws SamplingException {
    Metadata md = new Metadata();
    md.fromHexString(hexStr);
    setMap(md);
  }

  /**
   * Clears metadata for current thread.
   *
   * <p><b>Note:</b> This only clears the local thread storage. It does not affect any active
   * OpenTelemetry Scope.
   *
   * @deprecated Use OpenTelemetry Context propagation instead.
   */
  @Deprecated
  public static void clearMetadata() {
    setMap(new Metadata());
  }

  private static void setMap(Metadata md) {
    mdThreadLocal.set(md);
  }

  public static boolean isValid() {
    return getMetadata().isValid();
  }

  /**
   * Set whether the any child threads spawned by the current thread should skip inheriting a clone
   * of current context
   *
   * <p>It is known that in thread pool handling, using inheritable thread local has problem of
   * leaking context. (unable to clear the context in the spawned thread afterwards) This can be set
   * to true if the context propagation is handled by other means in order to avoid the leaking
   * problem.
   *
   * <p>By default this is false
   *
   * @param skipInheritingContext
   */
  public static void setSkipInheritingContext(boolean skipInheritingContext) {
    skipInheritingContextThreadLocal.set(skipInheritingContext);
  }
}
