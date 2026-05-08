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

package com.solarwinds.opentelemetry.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ContextPropagationTest {

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Test
  void verifyTraceContextPropagatesViaW3CHeaders() {
    testing.runWithSpan(
        "service-a",
        () -> {
          Map<String, String> headers = new HashMap<>();
          GlobalOpenTelemetry.getPropagators()
              .getTextMapPropagator()
              .inject(Context.current(), headers, Map::put);

          assertThat(headers).containsKey("traceparent");
          assertThat(headers.get("traceparent"))
              .matches("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}");

          assertThat(headers).containsKey("tracestate");
          assertThat(headers.get("tracestate")).contains("sw=");
        });
  }
}
