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

package com.solarwinds.joboe.sampling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import org.junit.jupiter.api.Test;

public class XtraceOptionsResponseTest {
  @Test
  public void testResponse() {
    TraceConfig traceConfig =
        new TraceConfig(
            TraceDecisionUtil.SAMPLE_RESOLUTION,
            SampleRateSource.OBOE_DEFAULT,
            TracingMode.ALWAYS.toFlags());
    XtraceOptions options;

    XTraceOptionsResponse response;
    // no x-trace options
    response =
        XTraceOptionsResponse.computeResponse(
            XtraceOptions.getXTraceOptions(null, null),
            new TraceDecision(true, true, traceConfig, TraceDecisionUtil.RequestType.REGULAR),
            true);
    assertNull(response);

    // empty x-trace options
    response =
        XTraceOptionsResponse.computeResponse(
            XtraceOptions.getXTraceOptions("", null),
            new TraceDecision(true, true, traceConfig, TraceDecisionUtil.RequestType.REGULAR),
            true);
    assertEquals("trigger-trace=not-requested", response.toString());

    // trigger trace (unauthenticated)
    options =
        new XtraceOptions(
            Collections.singletonMap(XtraceOption.TRIGGER_TRACE, true),
            Collections.emptyList(),
            XtraceOptions.AuthenticationStatus.NOT_AUTHENTICATED);
    response =
        XTraceOptionsResponse.computeResponse(
            options,
            new TraceDecision(
                true,
                true,
                traceConfig,
                TraceDecisionUtil.RequestType.UNAUTHENTICATED_TRIGGER_TRACE),
            true);
    assertEquals("trigger-trace=ok", response.toString());

    // trigger trace (authenticated)
    options =
        new XtraceOptions(
            Collections.singletonMap(XtraceOption.TRIGGER_TRACE, true),
            Collections.emptyList(),
            XtraceOptions.AuthenticationStatus.OK);
    response =
        XTraceOptionsResponse.computeResponse(
            options,
            new TraceDecision(
                true, true, traceConfig, TraceDecisionUtil.RequestType.AUTHENTICATED_TRIGGER_TRACE),
            true);
    assertEquals("auth=ok;trigger-trace=ok", response.toString());

    // trigger trace no remote settings
    options =
        new XtraceOptions(
            Collections.singletonMap(XtraceOption.TRIGGER_TRACE, true),
            Collections.emptyList(),
            XtraceOptions.AuthenticationStatus.NOT_AUTHENTICATED);
    response =
        XTraceOptionsResponse.computeResponse(
            options,
            new TraceDecision(
                false, false, null, TraceDecisionUtil.RequestType.UNAUTHENTICATED_TRIGGER_TRACE),
            true);
    assertEquals("trigger-trace=settings-not-available", response.toString());

    // trigger trace bucket exhausted
    options =
        new XtraceOptions(
            Collections.singletonMap(XtraceOption.TRIGGER_TRACE, true),
            Collections.emptyList(),
            XtraceOptions.AuthenticationStatus.NOT_AUTHENTICATED);
    response =
        XTraceOptionsResponse.computeResponse(
            options,
            new TraceDecision(
                false,
                false,
                true,
                traceConfig,
                TraceDecisionUtil.RequestType.UNAUTHENTICATED_TRIGGER_TRACE),
            true);
    assertEquals("trigger-trace=rate-exceeded", response.toString());

    // trigger trace trace mode = disabled
    options =
        new XtraceOptions(
            Collections.singletonMap(XtraceOption.TRIGGER_TRACE, true),
            Collections.emptyList(),
            XtraceOptions.AuthenticationStatus.NOT_AUTHENTICATED);
    TraceConfig tracingDisabledConfig =
        new TraceConfig(0, SampleRateSource.FILE, TracingMode.NEVER.toFlags());
    response =
        XTraceOptionsResponse.computeResponse(
            options,
            new TraceDecision(
                false,
                false,
                tracingDisabledConfig,
                TraceDecisionUtil.RequestType.UNAUTHENTICATED_TRIGGER_TRACE),
            true);
    assertEquals("trigger-trace=tracing-disabled", response.toString());

    // trigger trace feature is disabled
    options =
        new XtraceOptions(
            Collections.singletonMap(XtraceOption.TRIGGER_TRACE, true),
            Collections.emptyList(),
            XtraceOptions.AuthenticationStatus.NOT_AUTHENTICATED);
    TraceConfig featureDisabledConfig =
        new TraceConfig(
            TraceDecisionUtil.SAMPLE_RESOLUTION,
            SampleRateSource.FILE,
            (short)
                (TracingMode.ENABLED.toFlags()
                    & ~Settings.OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED));
    response =
        XTraceOptionsResponse.computeResponse(
            options,
            new TraceDecision(
                false,
                true,
                featureDisabledConfig,
                TraceDecisionUtil.RequestType.UNAUTHENTICATED_TRIGGER_TRACE),
            true);
    assertEquals("trigger-trace=trigger-tracing-disabled", response.toString());
  }

  @Test
  public void testExceptionResponse() {
    TraceConfig traceConfig =
        new TraceConfig(
            TraceDecisionUtil.SAMPLE_RESOLUTION,
            SampleRateSource.OBOE_DEFAULT,
            TracingMode.ALWAYS.toFlags());

    XTraceOptionsResponse response;
    // unknown X-Trace-Options
    response =
        XTraceOptionsResponse.computeResponse(
            XtraceOptions.getXTraceOptions(
                "unknown1=1;unknown2;" + XtraceOption.SW_KEYS.getKey() + "=3", null),
            new TraceDecision(true, true, traceConfig, TraceDecisionUtil.RequestType.REGULAR),
            true);
    assertEquals("trigger-trace=not-requested;ignored=unknown1,unknown2", response.toString());

    // invalid trigger-trace (has value)
    response =
        XTraceOptionsResponse.computeResponse(
            XtraceOptions.getXTraceOptions(
                XtraceOption.TRIGGER_TRACE.getKey() + "=0;" + XtraceOption.SW_KEYS.getKey() + "=3",
                null),
            new TraceDecision(true, true, traceConfig, TraceDecisionUtil.RequestType.REGULAR),
            true);
    assertEquals("trigger-trace=not-requested;ignored=trigger-trace", response.toString());
  }

  @Test
  public void testBadSignatureResponse() {
    TraceConfig traceConfig =
        new TraceConfig(
            TraceDecisionUtil.SAMPLE_RESOLUTION,
            SampleRateSource.OBOE_DEFAULT,
            TracingMode.ALWAYS.toFlags());

    XTraceOptionsResponse response;
    // bad timestamp
    XtraceOptions badTimestampOptions =
        new XtraceOptions(
            Collections.emptyMap(),
            Collections.emptyList(),
            XtraceOptions.AuthenticationStatus.failure("bad-timestamp"));
    response =
        XTraceOptionsResponse.computeResponse(
            badTimestampOptions,
            new TraceDecision(
                false, true, traceConfig, TraceDecisionUtil.RequestType.BAD_SIGNATURE),
            true);
    assertEquals("auth=bad-timestamp", response.toString());

    // bad signature
    XtraceOptions badSignatureOptions =
        new XtraceOptions(
            Collections.emptyMap(),
            Collections.emptyList(),
            XtraceOptions.AuthenticationStatus.failure("bad-signature"));
    response =
        XTraceOptionsResponse.computeResponse(
            badSignatureOptions,
            new TraceDecision(
                false, true, traceConfig, TraceDecisionUtil.RequestType.BAD_SIGNATURE),
            true);
    assertEquals("auth=bad-signature", response.toString());
  }
}
