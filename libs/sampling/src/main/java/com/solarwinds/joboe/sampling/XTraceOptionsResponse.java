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

import java.util.*;
import java.util.Map.Entry;

/**
 * Computes the response from {@link XtraceOptions} processing.
 *
 * <p>Contains the key/value produced from the computation.
 */
public class XTraceOptionsResponse {

  public static XTraceOptionsResponse computeResponse(
      XtraceOptions options, TraceDecision traceDecision, boolean isServiceRoot) {
    if (options == null) {
      return null;
    }

    XTraceOptionsResponse response = new XTraceOptionsResponse();

    if (options
        .getAuthenticationStatus()
        .isFailure()) { // if auth failure, we will only reply with the auth option
      response.setValue("auth", options.getAuthenticationStatus().getReason());
    } else {
      if (options.getAuthenticationStatus().isAuthenticated()) {
        response.setValue("auth", "ok");
      }
      boolean isTriggerTrace = options.getOptionValue(XtraceOption.TRIGGER_TRACE);
      if (isTriggerTrace) {
        if (!isServiceRoot) { // a continued trace, trigger trace flag has no effect
          response.setValue("trigger-trace", "ignored");
        } else if (traceDecision.isSampled()) {
          response.setValue("trigger-trace", "ok");
        } else if (traceDecision.getTraceConfig() == null) {
          response.setValue("trigger-trace", "settings-not-available");
        } else if (traceDecision.getTraceConfig().getFlags() == TracingMode.DISABLED.toFlags()) {
          response.setValue("trigger-trace", "tracing-disabled");
        } else if (!traceDecision.getTraceConfig().hasSampleTriggerTraceFlag()) {
          response.setValue("trigger-trace", "trigger-tracing-disabled");
        } else if (traceDecision.isBucketExhausted()) {
          response.setValue("trigger-trace", "rate-exceeded");
        } else {
          response.setValue("trigger-trace", "unknown-failure");
        }
      } else {
        response.setValue("trigger-trace", "not-requested");
      }

      for (XtraceOptions.XTraceOptionException exception : options.getExceptions()) {
        exception.appendToResponse(response);
      }
    }

    return response;
  }

  private final Map<String, String> keyValues = new LinkedHashMap<String, String>();

  private XTraceOptionsResponse() {}

  public String getValue(String key) {
    return keyValues.get(key);
  }

  public void setValue(String key, String value) {
    keyValues.put(key, value);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (Entry<String, String> entry : keyValues.entrySet()) {
      builder.append(entry.getKey() + "=" + entry.getValue() + ";");
    }

    if (builder.length() > 0) {
      builder.deleteCharAt(builder.length() - 1); // remove the last dangling ;
    }
    return builder.toString();
  }
}
