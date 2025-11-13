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

import com.google.auto.service.AutoService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.bootstrap.http.HttpServerResponseCustomizer;
import io.opentelemetry.javaagent.bootstrap.http.HttpServerResponseMutator;

@AutoService(HttpServerResponseCustomizer.class)
public class ResponseHeaderCustomizer implements HttpServerResponseCustomizer {
  private static final String XTRACE_HEADER =
      "X-Trace"; // used for trigger trace response header only

  private static final String SW_XTRACE_OPTIONS_RESP_KEY = "xtrace_options_response";

  private static final String XTRACE_OPTIONS_RESP_HEADER = "X-Trace-Options-Response";

  @Override
  public <RESPONSE> void customize(
      Context context,
      RESPONSE response,
      HttpServerResponseMutator<RESPONSE> httpServerResponseMutator) {
    SpanContext spanContext = Span.fromContext(context).getSpanContext();
    String flags = spanContext.isSampled() ? "01" : "00";
    String traceContext =
        "00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-" + flags;

    httpServerResponseMutator.appendHeader(response, XTRACE_HEADER, traceContext);
    String xtraceOptionsResp = spanContext.getTraceState().get(SW_XTRACE_OPTIONS_RESP_KEY);
    if (xtraceOptionsResp != null) {
      httpServerResponseMutator.appendHeader(
          response, XTRACE_OPTIONS_RESP_HEADER, recover(xtraceOptionsResp));
    }
  }

  private String recover(String in) {
    if (in == null) {
      return null;
    }
    return in.replace("####", "=").replace("....", ",");
  }
}
