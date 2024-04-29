package com.solarwinds.opentelemetry.instrumentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.IdGenerator;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TraceContextInjectorTest {

  @Mock private Span spanMock;

  @Mock private SpanContext spanContextMock;

  private final IdGenerator idGenerator = IdGenerator.random();

  private static final Set<String> activeDbs = new HashSet<>();

  @BeforeAll
  static void setup() {
    activeDbs.add("postgresql");
  }

  @Test
  void returnSqlWithTraceContextInjected() {
    String sql = "select name from students";
    String traceId = idGenerator.generateTraceId();
    String spanId = idGenerator.generateSpanId();

    try (MockedStatic<Span> spanMockedStatic = mockStatic(Span.class)) {
      spanMockedStatic.when(() -> Span.fromContext(any())).thenReturn(spanMock);
      when(spanMock.getSpanContext()).thenReturn(spanContextMock);

      when(spanContextMock.isValid()).thenReturn(true);
      when(spanContextMock.isSampled()).thenReturn(true);
      when(spanContextMock.getTraceId()).thenReturn(traceId);

      when(spanContextMock.getSpanId()).thenReturn(spanId);

      String actual = TraceContextInjector.inject(Context.current(), sql);
      String expected = String.format("/*traceparent='00-%s-%s-01'*/ %s", traceId, spanId, sql);
      assertEquals(expected, actual);
    }
  }

  @Test
  void returnSqlWithoutTraceContextWhenSpanIsNotSampled() {
    String sql = "select name from students";

    try (MockedStatic<Span> spanMockedStatic = mockStatic(Span.class)) {
      spanMockedStatic.when(() -> Span.fromContext(any())).thenReturn(spanMock);
      when(spanMock.getSpanContext()).thenReturn(spanContextMock);

      when(spanContextMock.isValid()).thenReturn(true);
      when(spanContextMock.isSampled()).thenReturn(false);

      String actual = TraceContextInjector.inject(Context.current(), sql);
      assertEquals(sql, actual);
    }
  }

  @Test
  void returnTrueWhenDbIsNotConfiguredAndInputIsMysql() {
    ConfigManager.removeConfig(ConfigProperty.AGENT_SQL_TAG_DATABASES);
    assertTrue(TraceContextInjector.isDbConfigured(TraceContextInjector.Db.mysql));
  }

  @Test
  void returnTrueWhenDbIsConfiguredAndInputIsPostgresql() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_SQL_TAG_DATABASES, activeDbs);
    assertTrue(TraceContextInjector.isDbConfigured(TraceContextInjector.Db.postgresql));
  }

  @Test
  void returnFalseWhenDbIsNotConfigured() {
    ConfigManager.removeConfig(ConfigProperty.AGENT_SQL_TAG_DATABASES);
    assertFalse(TraceContextInjector.isDbConfigured(TraceContextInjector.Db.postgresql));
  }
}
