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

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.internal.ExtendedSpanProcessor;
import io.opentelemetry.semconv.CodeAttributes;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StacktraceComponentProviderTest {

  private final StacktraceComponentProvider tested = new StacktraceComponentProvider();

  @AfterEach
  void tearDown() {
    ConfigManager.reset();
  }

  @Test
  void testName() {
    assertEquals("swo_stacktrace", tested.getName());
  }

  @Test
  void negativeMinDurationDisablesStacktraceCapture() {
    DeclarativeConfigProperties config = config(-1, null);
    ReadWriteSpan span = span(Long.MAX_VALUE, "db.system");

    SpanProcessor processor = tested.create(config);
    assertTrue(!processor.isEndRequired() && !processor.isStartRequired());
    end(processor, span);

    verify(span, never()).setAttribute(eq(CodeAttributes.CODE_STACKTRACE), anyString());
  }

  @Test
  void capturesStacktraceForDefaultFilterAttribute() {
    ReadWriteSpan span = span(TimeUnit.MILLISECONDS.toNanos(10), "db.system");

    end(tested.create(config(5, null)), span);
    verify(span).setAttribute(eq(CodeAttributes.CODE_STACKTRACE), anyString());
  }

  @Test
  void capturesStacktraceForAttributeInSelectorsProperty() {
    DeclarativeConfigProperties config = config(5, null);
    when(config.getScalarList(eq("selectors"), eq(String.class), anyList()))
        .thenReturn(Collections.singletonList("http.request.method"));
    ReadWriteSpan span = span(TimeUnit.MILLISECONDS.toNanos(10), "http.request.method");

    end(tested.create(config), span);
    verify(span).setAttribute(eq(CodeAttributes.CODE_STACKTRACE), anyString());
  }

  @Test
  void ignoresLegacyAgentFilterConfig() throws InvalidConfigException {
    ConfigManager.setConfig(
        ConfigProperty.AGENT_SPAN_STACKTRACE_FILTERS, Collections.singleton("process.pid"));
    ReadWriteSpan span = span(TimeUnit.MILLISECONDS.toNanos(10), "process.pid");

    end(tested.create(config(5, null)), span);
    verify(span, never()).setAttribute(eq(CodeAttributes.CODE_STACKTRACE), anyString());
  }

  @Test
  void skipsSpansWithoutFilterAttribute() {
    ReadWriteSpan span = span(TimeUnit.MILLISECONDS.toNanos(10), "unrelated.attribute");

    end(tested.create(config(5, null)), span);
    verify(span, never()).setAttribute(eq(CodeAttributes.CODE_STACKTRACE), anyString());
  }

  @Test
  void skipsSpansShorterThanMinDuration() {
    ReadWriteSpan span = span(TimeUnit.MILLISECONDS.toNanos(1), "db.system");

    end(tested.create(config(5, null)), span);
    verify(span, never()).setAttribute(eq(CodeAttributes.CODE_STACKTRACE), anyString());
  }

  @Test
  void zeroMinDurationCapturesEveryMatchingSpan() {
    ReadWriteSpan span = span(0, "db.system");

    end(tested.create(config(0, null)), span);
    verify(span).setAttribute(eq(CodeAttributes.CODE_STACKTRACE), anyString());
  }

  @Test
  void usesUserSuppliedFilterInsteadOfDefault() {
    ReadWriteSpan span = span(TimeUnit.MILLISECONDS.toNanos(10), "unrelated.attribute");

    end(tested.create(config(5, AcceptAllFilter.class.getName())), span);
    verify(span).setAttribute(eq(CodeAttributes.CODE_STACKTRACE), anyString());
  }

  public static class AcceptAllFilter implements Predicate<ReadableSpan> {
    @Override
    public boolean test(ReadableSpan span) {
      return true;
    }
  }

  private static void end(SpanProcessor processor, ReadWriteSpan span) {
    if (processor instanceof ExtendedSpanProcessor) {
      ((ExtendedSpanProcessor) processor).onEnding(span);
    }
  }

  private static DeclarativeConfigProperties config(long minDuration, String filter) {
    DeclarativeConfigProperties config = mock(DeclarativeConfigProperties.class);
    when(config.getLong(eq("min_duration"), anyLong())).thenReturn(minDuration);

    when(config.getString("filter")).thenReturn(filter);
    when(config.getScalarList(eq("selectors"), eq(String.class), anyList()))
        .thenAnswer(i -> i.getArgument(2));
    return config;
  }

  private static ReadWriteSpan span(long latencyNanos, String attributeKey) {
    ReadWriteSpan span = mock(ReadWriteSpan.class);
    when(span.getLatencyNanos()).thenReturn(latencyNanos);

    when(span.getAttributes()).thenReturn(Attributes.of(stringKey(attributeKey), "v"));
    when(span.getInstrumentationScopeInfo()).thenReturn(InstrumentationScopeInfo.create("test"));
    return span;
  }
}
