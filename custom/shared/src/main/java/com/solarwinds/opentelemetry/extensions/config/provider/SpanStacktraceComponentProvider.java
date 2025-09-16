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

import static com.solarwinds.opentelemetry.extensions.SharedNames.SPAN_STACKTRACE_FILTER_CLASS;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.stacktrace.StackTraceSpanProcessor;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Predicate;
import javax.annotation.Nullable;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class SpanStacktraceComponentProvider implements ComponentProvider<SpanProcessor> {

  public static final String COMPONENT_NAME = "swo/spanStacktrace";

  private final Logger logger = LoggerFactory.getLogger();

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
    long duration = declarativeConfigProperties.getLong("duration", 5);
    return new StackTraceSpanProcessor(duration, getFilterPredicate(declarativeConfigProperties));
  }

  Predicate<ReadableSpan> getFilterPredicate(
      DeclarativeConfigProperties declarativeConfigProperties) {
    String filterClass =
        declarativeConfigProperties.getString("filterClass", SPAN_STACKTRACE_FILTER_CLASS);
    Predicate<ReadableSpan> filter = null;
    if (filterClass != null) {
      Class<?> filterType = getFilterType(filterClass);
      if (filterType != null) {
        filter = getFilterInstance(filterType);
      }
    }

    if (filter == null) {
      // if value is set, lack of filtering is likely an error and must be reported
      Logger.Level disabledLogLevel = filterClass != null ? Logger.Level.FATAL : Logger.Level.TRACE;
      logger.log(disabledLogLevel, "Span stacktrace filtering disabled");
      return span -> true;
    } else {
      logger.trace("Span stacktrace filtering enabled with: " + filterClass);
      return filter;
    }
  }

  @Nullable private Class<?> getFilterType(String filterClass) {
    try {
      Class<?> filterType = Class.forName(filterClass);
      if (!Predicate.class.isAssignableFrom(filterType)) {
        logger.error("Filter must be a subclass of java.util.function.Predicate");
        return null;
      }
      return filterType;
    } catch (ClassNotFoundException e) {
      logger.error("Unable to load filter class: " + filterClass);
      return null;
    }
  }

  @Nullable @SuppressWarnings("unchecked")
  private Predicate<ReadableSpan> getFilterInstance(Class<?> filterType) {
    try {
      Constructor<?> constructor = filterType.getConstructor();
      return (Predicate<ReadableSpan>) constructor.newInstance();
    } catch (NoSuchMethodException
        | InstantiationException
        | IllegalAccessException
        | InvocationTargetException e) {
      logger.error("Unable to create filter instance with no-arg constructor: " + filterType);
      return null;
    }
  }
}
