package com.solarwinds.opentelemetry.extensions;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.core.TraceDecision;
import com.solarwinds.joboe.core.TraceDecisionUtil;
import com.solarwinds.joboe.core.XTraceOptions;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SamplingUtilTest {

  @Mock private TraceDecision traceDecisionMock;

  @Test
  void verifyThatTriggeredTraceAttributeIsAddedForAuthenticatedTriggerTrace() {
    AttributesBuilder builder = Attributes.builder();
    XTraceOptions xTraceOptions = XTraceOptions.getXTraceOptions("trigger-trace", null);
    when(traceDecisionMock.getRequestType())
        .thenReturn(TraceDecisionUtil.RequestType.AUTHENTICATED_TRIGGER_TRACE);

    SamplingUtil.addXtraceOptionsToAttribute(traceDecisionMock, xTraceOptions, builder);
    assertEquals(Boolean.TRUE, builder.build().get(booleanKey("TriggeredTrace")));
  }

  @Test
  void verifyThatTriggeredTraceAttributeIsAddedForUnauthenticatedTriggerTrace() {
    AttributesBuilder builder = Attributes.builder();
    XTraceOptions xTraceOptions = XTraceOptions.getXTraceOptions("trigger-trace", null);
    when(traceDecisionMock.getRequestType())
        .thenReturn(TraceDecisionUtil.RequestType.UNAUTHENTICATED_TRIGGER_TRACE);

    SamplingUtil.addXtraceOptionsToAttribute(traceDecisionMock, xTraceOptions, builder);
    assertEquals(Boolean.TRUE, builder.build().get(booleanKey("TriggeredTrace")));
  }

  @Test
  void verifyThatCustomKvAttributesAreAdded() {
    AttributesBuilder builder = Attributes.builder();
    XTraceOptions xTraceOptions = XTraceOptions.getXTraceOptions("custom-chubi=chubby;", null);
    when(traceDecisionMock.getRequestType())
        .thenReturn(TraceDecisionUtil.RequestType.UNAUTHENTICATED_TRIGGER_TRACE);

    SamplingUtil.addXtraceOptionsToAttribute(traceDecisionMock, xTraceOptions, builder);
    assertEquals("chubby", builder.build().get(stringKey("custom-chubi")));
  }

  @Test
  void verifyThatSwKeysAttributeIsAdded() {
    AttributesBuilder builder = Attributes.builder();
    XTraceOptions xTraceOptions =
        XTraceOptions.getXTraceOptions("sw-keys=lo:se,check-id:123", null);
    when(traceDecisionMock.getRequestType())
        .thenReturn(TraceDecisionUtil.RequestType.AUTHENTICATED_TRIGGER_TRACE);

    SamplingUtil.addXtraceOptionsToAttribute(traceDecisionMock, xTraceOptions, builder);
    assertEquals("lo:se,check-id:123", builder.build().get(stringKey("SWKeys")));
  }

  @Test
  void returnTrueGivenValidSwTraceState() {
    String swTraceState = "4025843a0f1f35f3-01";
    assertTrue(SamplingUtil.isValidSwTraceState(swTraceState));
  }

  @Test
  void returnFalseGivenSwTraceStateWithInvalidFlag() {
    String swTraceState = "4025843a0f1f35f3-11";
    assertFalse(SamplingUtil.isValidSwTraceState(swTraceState));
  }

  @Test
  void returnFalseGivenSwTraceStateWithSpanIdLengthLessThan16() {
    String swTraceState = "4025843a0f1f35f-01";
    assertFalse(SamplingUtil.isValidSwTraceState(swTraceState));
  }

  @Test
  void returnFalseGivenSwTraceStateWithSpanIdLengthGreaterThan16() {
    String swTraceState = "4025843a0f1f35f33-01";
    assertFalse(SamplingUtil.isValidSwTraceState(swTraceState));
  }

  @Test
  void returnFalseGivenSwTraceStateWithInvalidFormat() {
    String swTraceState = "4025843a0f1f3-5f-01";
    assertFalse(SamplingUtil.isValidSwTraceState(swTraceState));
  }
}