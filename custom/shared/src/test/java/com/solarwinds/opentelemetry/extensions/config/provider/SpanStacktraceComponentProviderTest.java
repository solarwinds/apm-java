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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpanStacktraceComponentProviderTest {
  private final SpanStacktraceComponentProvider tested = new SpanStacktraceComponentProvider();

  @Mock private DeclarativeConfigProperties declarativeConfigPropertiesMock;

  @Test
  void testName() {
    assertEquals("swo/spanStacktrace", tested.getName());
  }

  @Test
  void customFilter() {
    when(declarativeConfigPropertiesMock.getString(eq("filterClass"), any()))
        .thenReturn(CustomFilter.class.getName());

    Predicate<ReadableSpan> filterPredicate =
        tested.getFilterPredicate(declarativeConfigPropertiesMock);

    assertInstanceOf(CustomFilter.class, filterPredicate);
    assertFalse(filterPredicate.test(null));
  }

  @Test
  void testBrokenFilter() {
    when(declarativeConfigPropertiesMock.getString(eq("filterClass"), any())).thenReturn("broken");
    Predicate<ReadableSpan> filterPredicate =
        tested.getFilterPredicate(declarativeConfigPropertiesMock);

    assertNotNull(filterPredicate);
    assertTrue(filterPredicate.test(null));
  }

  public static class CustomFilter implements Predicate<ReadableSpan> {
    @Override
    public boolean test(ReadableSpan readableSpan) {
      return false;
    }
  }
}
