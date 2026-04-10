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

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.sampling.TraceDecision;
import com.solarwinds.joboe.sampling.TraceDecisionUtil;
import com.solarwinds.joboe.sampling.XtraceOptions;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SamplingUtilTest {

  @Mock private TraceDecision traceDecisionMock;

  @Test
  void verifyThatTriggeredTraceAttributeIsAddedForAuthenticatedTriggerTrace() {
    AttributesBuilder builder = Attributes.builder();
    XtraceOptions xTraceOptions = XtraceOptions.getXTraceOptions("trigger-trace", null);
    when(traceDecisionMock.getRequestType())
        .thenReturn(TraceDecisionUtil.RequestType.AUTHENTICATED_TRIGGER_TRACE);

    SamplingUtil.addXtraceOptionsToAttribute(traceDecisionMock, xTraceOptions, builder);
    assertEquals(Boolean.TRUE, builder.build().get(booleanKey("TriggeredTrace")));
  }

  @Test
  void verifyThatTriggeredTraceAttributeIsAddedForUnauthenticatedTriggerTrace() {
    AttributesBuilder builder = Attributes.builder();
    XtraceOptions xTraceOptions = XtraceOptions.getXTraceOptions("trigger-trace", null);
    when(traceDecisionMock.getRequestType())
        .thenReturn(TraceDecisionUtil.RequestType.UNAUTHENTICATED_TRIGGER_TRACE);

    SamplingUtil.addXtraceOptionsToAttribute(traceDecisionMock, xTraceOptions, builder);
    assertEquals(Boolean.TRUE, builder.build().get(booleanKey("TriggeredTrace")));
  }

  @Test
  void verifyThatCustomKvAttributesAreAdded() {
    AttributesBuilder builder = Attributes.builder();
    XtraceOptions xTraceOptions = XtraceOptions.getXTraceOptions("custom-chubi=chubby;", null);
    when(traceDecisionMock.getRequestType())
        .thenReturn(TraceDecisionUtil.RequestType.UNAUTHENTICATED_TRIGGER_TRACE);

    SamplingUtil.addXtraceOptionsToAttribute(traceDecisionMock, xTraceOptions, builder);
    assertEquals("chubby", builder.build().get(stringKey("custom-chubi")));
  }

  @Test
  void verifyThatSwKeysAttributeIsAdded() {
    AttributesBuilder builder = Attributes.builder();
    XtraceOptions xTraceOptions =
        XtraceOptions.getXTraceOptions("sw-keys=lo:se,check-id:123", null);
    when(traceDecisionMock.getRequestType())
        .thenReturn(TraceDecisionUtil.RequestType.AUTHENTICATED_TRIGGER_TRACE);

    SamplingUtil.addXtraceOptionsToAttribute(traceDecisionMock, xTraceOptions, builder);
    assertEquals("lo:se,check-id:123", builder.build().get(stringKey("SWKeys")));
  }

  static Stream<String> validHexFlags() {
    return IntStream.rangeClosed(0x00, 0xff)
        .mapToObj(i -> "4025843a0f1f35f3-" + String.format("%02x", i));
  }

  @ParameterizedTest
  @MethodSource("validHexFlags")
  void returnTrueGivenValidSwTraceStateForAllHexFlags(String swTraceState) {
    assertTrue(SamplingUtil.isValidSwTraceState(swTraceState));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        "4025843a0f1f35f3-0g",
        "4025843a0f1f35f-01",
        "4025843a0f1f35f33-01",
        "4025843a0f1f3-5f-01"
      })
  void returnFalseGivenInvalidSwTraceState(String swTraceState) {
    assertFalse(SamplingUtil.isValidSwTraceState(swTraceState));
  }
}
