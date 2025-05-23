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

package com.solarwinds.opentelemetry.instrumentation.servlet.v3_0;

import com.solarwinds.joboe.logging.LoggerFactory;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import net.bytebuddy.asm.Advice;

@SuppressWarnings("unused")
public class Servlet3Advice {
  private static final String XTRACE_HEADER =
      "X-Trace"; // used for trigger trace response header only
  private static final String SW_XTRACE_OPTIONS_RESP_KEY = "xtrace_options_response";
  private static final String XTRACE_OPTIONS_RESP_HEADER = "X-Trace-Options-Response";

  @Advice.OnMethodEnter(suppress = Throwable.class)
  public static void onEnter(
      @Advice.Argument(value = 1, readOnly = false) ServletResponse response) {
    if (response instanceof HttpServletResponse) {
      HttpServletResponse httpServletResponse = (HttpServletResponse) response;
      if (!response.isCommitted() && !httpServletResponse.containsHeader(XTRACE_HEADER)) {
        injectXtraceHeader(httpServletResponse);
      } else if (response.isCommitted()) {
        LoggerFactory.getLogger()
            .info(
                "[Servlet 3] Did not add x-trace header because ServletResponse has been committed");
      }
    }
  }

  public static void injectXtraceHeader(HttpServletResponse response) {
    SpanContext spanContext = Span.fromContext(Context.current()).getSpanContext();
    String flags = spanContext.isSampled() ? "01" : "00";
    String traceContext =
        "00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-" + flags;
    response.addHeader(XTRACE_HEADER, traceContext);

    String xtraceOptionsResp = spanContext.getTraceState().get(SW_XTRACE_OPTIONS_RESP_KEY);
    if (xtraceOptionsResp != null) {
      response.addHeader(XTRACE_OPTIONS_RESP_HEADER, recover(xtraceOptionsResp));
    }
  }

  public static String recover(String in) {
    if (in == null) {
      return in;
    }
    return in.replace("####", "=").replace("....", ",");
  }
}
