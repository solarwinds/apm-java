package com.appoptics.opentelemetry.extensions;

import static com.appoptics.opentelemetry.extensions.SamplingUtil.SW_TRACESTATE_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.appoptics.opentelemetry.core.Util;
import com.solarwinds.joboe.core.TraceConfig;
import com.solarwinds.joboe.core.TraceDecision;
import com.solarwinds.joboe.core.TraceDecisionUtil;
import com.solarwinds.joboe.core.XTraceOptions;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppOpticsSamplerTest {

  @InjectMocks private AppOpticsSampler tested;

  @Mock private TraceDecision traceDecisionMock;

  @Mock private XTraceOptions xTraceOptionsMock;

  @Mock private TraceConfig traceConfigMock;

  @Mock private SpanContext spanContextMock;

  @Mock private Span spanMock;

  @Mock private TraceState traceStateMock;

  @Captor private ArgumentCaptor<String> stringArgumentCaptor;

  private final IdGenerator idGenerator = IdGenerator.random();

  @Test
  void returnSamplingResultGivenTraceDecisionIsSampled() {
    when(traceDecisionMock.isSampled()).thenReturn(true);
    when(traceDecisionMock.getTraceConfig()).thenReturn(traceConfigMock);
    when(traceConfigMock.getSampleRate()).thenReturn(100);

    when(traceConfigMock.getSampleRateSourceValue()).thenReturn(2);
    when(traceConfigMock.getBucketRate(any())).thenReturn(0.5);
    when(traceConfigMock.getBucketCapacity(any())).thenReturn(0.5);

    when(traceDecisionMock.getRequestType()).thenReturn(TraceDecisionUtil.RequestType.REGULAR);
    when(traceDecisionMock.isReportMetrics()).thenReturn(true);

    tested.toOtSamplingResult(traceDecisionMock, xTraceOptionsMock, false);

    verify(traceDecisionMock, atLeastOnce()).getTraceConfig();
    verify(traceConfigMock, atLeastOnce()).getSampleRate();
  }

  @Test
  void returnSamplingResultGivenTraceDecisionIsMetricsOnly() {
    when(traceDecisionMock.isSampled()).thenReturn(false);
    when(traceDecisionMock.isReportMetrics()).thenReturn(true);

    SamplingResult actual = tested.toOtSamplingResult(traceDecisionMock, xTraceOptionsMock, false);
    assertEquals(AppOpticsSampler.METRICS_ONLY, actual);
  }

  @Test
  void returnSamplingResultGivenTraceDecisionIsNotSample() {
    when(traceDecisionMock.isSampled()).thenReturn(false);
    when(traceDecisionMock.isReportMetrics()).thenReturn(false);

    SamplingResult actual = tested.toOtSamplingResult(traceDecisionMock, xTraceOptionsMock, false);
    assertEquals(AppOpticsSampler.NOT_TRACED, actual);
  }

  @Test
  void verifyThatLocalTraceDecisionMachineryIsUsedWhenSpanIsRoot() {
    try (MockedStatic<Span> spanMockedStatic = mockStatic(Span.class);
        MockedStatic<TraceDecisionUtil> traceDecisionUtilMockedStatic =
            mockStatic(TraceDecisionUtil.class)) {
      spanMockedStatic.when(() -> Span.fromContext(any())).thenReturn(spanMock);
      traceDecisionUtilMockedStatic
          .when(
              () ->
                  TraceDecisionUtil.shouldTraceRequest(
                      any(), stringArgumentCaptor.capture(), any(), any()))
          .thenReturn(traceDecisionMock);

      when(spanContextMock.isValid()).thenReturn(false);
      when(traceDecisionMock.isSampled()).thenReturn(false);
      when(traceDecisionMock.isReportMetrics()).thenReturn(false);

      when(spanMock.getSpanContext()).thenReturn(spanContextMock);
      tested.shouldSample(
          Context.current(),
          idGenerator.generateTraceId(),
          "name",
          SpanKind.INTERNAL,
          Attributes.empty(),
          List.of());

      traceDecisionUtilMockedStatic.verify(
          () -> TraceDecisionUtil.shouldTraceRequest(any(), any(), any(), any()));
      assertNull(stringArgumentCaptor.getValue());
    }
  }

  @Test
  void verifyThatLocalTraceDecisionMachineryIsUsedWhenSpanIsNotRootAndSwTraceStateIsInvalid() {
    try (MockedStatic<Span> spanMockedStatic = mockStatic(Span.class);
        MockedStatic<TraceDecisionUtil> traceDecisionUtilMockedStatic =
            mockStatic(TraceDecisionUtil.class)) {
      spanMockedStatic.when(() -> Span.fromContext(any())).thenReturn(spanMock);
      traceDecisionUtilMockedStatic
          .when(() -> TraceDecisionUtil.shouldTraceRequest(any(), any(), any(), any()))
          .thenReturn(traceDecisionMock);

      when(spanContextMock.isValid()).thenReturn(true);
      when(traceDecisionMock.isSampled()).thenReturn(false);
      when(traceDecisionMock.isReportMetrics()).thenReturn(false);

      when(spanMock.getSpanContext()).thenReturn(spanContextMock);
      when(spanContextMock.getTraceState()).thenReturn(traceStateMock);
      when(traceStateMock.get(any())).thenReturn("this is illegal");

      tested.shouldSample(
          Context.current(),
          idGenerator.generateTraceId(),
          "name",
          SpanKind.INTERNAL,
          Attributes.empty(),
          List.of());

      verify(traceStateMock).get(stringArgumentCaptor.capture());
      traceDecisionUtilMockedStatic.verify(
          () -> TraceDecisionUtil.shouldTraceRequest(any(), any(), any(), any()));

      assertEquals(SW_TRACESTATE_KEY, stringArgumentCaptor.getValue());
    }
  }

  @Test
  void verifyThatLocalTraceDecisionMachineryIsUsedWhenSpanIsRemoteAndSwTraceStateIsValid() {
    try (MockedStatic<Span> spanMockedStatic = mockStatic(Span.class);
        MockedStatic<TraceDecisionUtil> traceDecisionUtilMockedStatic =
            mockStatic(TraceDecisionUtil.class);
        MockedStatic<Util> utilMockedStatic = mockStatic(Util.class)) {
      spanMockedStatic.when(() -> Span.fromContext(any())).thenReturn(spanMock);
      traceDecisionUtilMockedStatic
          .when(() -> TraceDecisionUtil.shouldTraceRequest(any(), any(), any(), any()))
          .thenReturn(traceDecisionMock);

      String traceId = idGenerator.generateTraceId();
      utilMockedStatic.when(() -> Util.w3cContextToHexString(spanContextMock)).thenReturn(traceId);

      String spanId = idGenerator.generateSpanId();
      String swVal = String.format("%s-%s", spanId, "01");
      when(spanContextMock.isRemote()).thenReturn(true);

      when(spanContextMock.isValid()).thenReturn(true);
      when(traceDecisionMock.isSampled()).thenReturn(false);
      when(traceDecisionMock.isReportMetrics()).thenReturn(false);

      when(spanMock.getSpanContext()).thenReturn(spanContextMock);
      when(spanContextMock.getTraceState()).thenReturn(traceStateMock);
      when(traceStateMock.get(any())).thenReturn(swVal);

      tested.shouldSample(
          Context.current(),
          idGenerator.generateTraceId(),
          "name",
          SpanKind.INTERNAL,
          Attributes.empty(),
          List.of());

      traceDecisionUtilMockedStatic.verify(
          () ->
              TraceDecisionUtil.shouldTraceRequest(
                  any(), stringArgumentCaptor.capture(), any(), any()));
      utilMockedStatic.verify(() -> Util.w3cContextToHexString(spanContextMock));

      assertEquals(traceId, stringArgumentCaptor.getValue());
    }
  }

  @Test
  void
      returnRecordAndSampleDecisionWhenLocalTraceDecisionMachineryIsNotUsedAndSpanIsLocalAndSwTraceStateIsSample() {
    try (MockedStatic<Span> spanMockedStatic = mockStatic(Span.class); ) {
      spanMockedStatic.when(() -> Span.fromContext(any())).thenReturn(spanMock);

      String spanId = idGenerator.generateSpanId();
      String swVal = String.format("%s-%s", spanId, "01");
      when(spanContextMock.isRemote()).thenReturn(false);

      when(spanContextMock.isValid()).thenReturn(true);
      when(spanMock.getSpanContext()).thenReturn(spanContextMock);
      when(spanContextMock.getTraceState()).thenReturn(traceStateMock);

      when(traceStateMock.get(any())).thenReturn(swVal);
      SamplingResult actual =
          tested.shouldSample(
              Context.current(),
              idGenerator.generateTraceId(),
              "name",
              SpanKind.INTERNAL,
              Attributes.empty(),
              List.of());

      assertEquals(SamplingDecision.RECORD_AND_SAMPLE, actual.getDecision());
    }
  }

  @Test
  void
      returnDropDecisionWhenLocalTraceDecisionMachineryIsNotUsedAndSpanIsLocalAndSwTraceStateIsNotSample() {
    try (MockedStatic<Span> spanMockedStatic = mockStatic(Span.class); ) {
      spanMockedStatic.when(() -> Span.fromContext(any())).thenReturn(spanMock);

      String spanId = idGenerator.generateSpanId();
      String swVal = String.format("%s-%s", spanId, "00");
      when(spanContextMock.isRemote()).thenReturn(false);

      when(spanContextMock.isValid()).thenReturn(true);
      when(spanMock.getSpanContext()).thenReturn(spanContextMock);
      when(spanContextMock.getTraceState()).thenReturn(traceStateMock);

      when(traceStateMock.get(any())).thenReturn(swVal);
      SamplingResult actual =
          tested.shouldSample(
              Context.current(),
              idGenerator.generateTraceId(),
              "name",
              SpanKind.INTERNAL,
              Attributes.empty(),
              List.of());

      assertEquals(SamplingDecision.DROP, actual.getDecision());
    }
  }
}
