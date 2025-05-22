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
class SpanStacktraceProviderTest {
  private final SpanStacktraceProvider tested = new SpanStacktraceProvider();

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
