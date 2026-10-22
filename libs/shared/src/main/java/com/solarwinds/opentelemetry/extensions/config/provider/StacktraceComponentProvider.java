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

package com.solarwinds.opentelemetry.extensions.config.provider;

import com.google.auto.service.AutoService;
import com.solarwinds.opentelemetry.extensions.SpanStacktraceFilter;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.stacktrace.StackTraceComponentProvider;
import io.opentelemetry.contrib.stacktrace.StackTraceSpanProcessor;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Drop-in replacement for {@code stacktrace/development} that defaults the span filter to {@link
 * SpanStacktraceFilter}. The span attribute keys that trigger capture are set with {@code filters};
 * {@code db.system} always applies.
 */
@AutoService(ComponentProvider.class)
public class StacktraceComponentProvider implements ComponentProvider {

  public static final String COMPONENT_NAME = "swo_stacktrace";

  private static final long DEFAULT_MIN_DURATION_MILLIS = 5;

  @Override
  public Class<SpanProcessor> getType() {
    return SpanProcessor.class;
  }

  @Override
  public String getName() {
    return COMPONENT_NAME;
  }

  @Override
  public SpanProcessor create(DeclarativeConfigProperties declarativeConfigProperties) {
    long minDurationMillis =
        declarativeConfigProperties.getLong("min_duration", DEFAULT_MIN_DURATION_MILLIS);

    if (minDurationMillis < 0) {
      return SpanProcessor.composite();
    }

    if (declarativeConfigProperties.getString("filter") != null) {
      return new StackTraceComponentProvider().create(declarativeConfigProperties);
    }

    List<String> filters =
        declarativeConfigProperties.getScalarList("filters", String.class, Collections.emptyList());
    SpanStacktraceFilter filter = new SpanStacktraceFilter(filters);

    return new StackTraceSpanProcessor(TimeUnit.MILLISECONDS.toNanos(minDurationMillis), filter);
  }
}
