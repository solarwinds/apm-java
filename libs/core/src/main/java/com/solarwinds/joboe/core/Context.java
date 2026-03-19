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

import com.solarwinds.joboe.sampling.Metadata;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

public class Context {

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
    SpanContext sc = Span.current().getSpanContext();
    return new Metadata(sc);
  }

  public static boolean isValid() {
    return getMetadata().isValid();
  }
}
