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

package com.solarwinds.opentelemetry.extensions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpanStacktraceFilterTest {

  private SpanStacktraceFilter tested;

  @BeforeEach
  void setUp() {
    tested = new SpanStacktraceFilter();
  }

  @AfterEach
  void tearDown() {
    ConfigManager.reset();
  }

  @Test
  void returnTrueWhenSpanHasMultipleMatchingAttributes() {
    ReadableSpan span =
        createSpanWithAttributes(
            Attributes.builder().put("db.system", "postgresql").put("http.method", "GET").build());
    assertTrue(tested.test(span));
  }

  @Test
  void returnFalseWhenSpanHasNoAttributes() {
    ReadableSpan span = createSpanWithAttributes(Attributes.empty());
    assertFalse(tested.test(span));
  }

  @Test
  void returnTrueWhenSpanHasCustomConfiguredLongAttribute() throws InvalidConfigException {
    Set<String> customFilters = new HashSet<>();
    customFilters.add("process.id");
    ConfigManager.setConfig(ConfigProperty.AGENT_SPAN_STACKTRACE_FILTERS, customFilters);

    SpanStacktraceFilter filter = new SpanStacktraceFilter();
    ReadableSpan span =
        createSpanWithAttributes(Attributes.builder().put("process.id", 450).build());

    assertTrue(filter.test(span));
  }

  @Test
  void returnTrueWhenSpanHasCustomConfiguredStringArrayAttribute() throws InvalidConfigException {
    Set<String> customFilters = new HashSet<>();
    customFilters.add("messaging.systems");
    ConfigManager.setConfig(ConfigProperty.AGENT_SPAN_STACKTRACE_FILTERS, customFilters);

    SpanStacktraceFilter filter = new SpanStacktraceFilter();
    ReadableSpan spanWithMessaging =
        createSpanWithAttributes(
            Attributes.builder()
                .put(
                    AttributeKey.stringArrayKey("messaging.systems"),
                    Arrays.asList("postgresql", "rabbitmq"))
                .build());

    assertTrue(filter.test(spanWithMessaging));
  }

  @Test
  void returnTrueWhenSpanHasCustomConfiguredLongArrayAttribute() throws InvalidConfigException {
    Set<String> customFilters = new HashSet<>();
    customFilters.add("process.ids");
    ConfigManager.setConfig(ConfigProperty.AGENT_SPAN_STACKTRACE_FILTERS, customFilters);

    SpanStacktraceFilter filter = new SpanStacktraceFilter();
    ReadableSpan span =
        createSpanWithAttributes(
            Attributes.builder()
                .put(AttributeKey.longArrayKey("process.ids"), Arrays.asList(200L, 300L))
                .build());

    assertTrue(filter.test(span));
  }

  private ReadableSpan createSpanWithAttributes(Attributes attributes) {
    ReadableSpan span = mock(ReadableSpan.class);
    when(span.getAttributes()).thenReturn(attributes);
    return span;
  }
}
