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

package com.solarwinds.opentelemetry.instrumentation.feature.test;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ProfilingSpanProcessorTest {

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Test
  void verifyProfilingSpanProcessorIsRegistered() {
    testing.runWithSpan("profiling-test", () -> {});

    List<List<SpanData>> traces = testing.waitForTraces(1);
    assertThat(traces).isNotEmpty();

    SpanData span = traces.get(0).get(0);
    Long profileSpans = span.getAttributes().get(AttributeKey.longKey("sw.profile.spans"));
    assertThat(profileSpans)
        .as("sw.profile.spans attribute should be set by SolarwindsProfilingSpanProcessor")
        .isNotNull();
  }

  @Test
  void verifyProfilesIsCollected() {
    testing.runWithSpan(
        "profiling-test",
        () -> {
          try {
            Thread.sleep(2000);
          } catch (InterruptedException ignored) {
          }
        });

    List<List<SpanData>> traces = testing.waitForTraces(1);
    assertThat(traces).isNotEmpty();

    SpanData span = traces.get(0).get(0);
    Long profileSpans = span.getAttributes().get(AttributeKey.longKey("sw.profile.spans"));
    assertThat(profileSpans).as("sw.profile.spans attribute should be set 1").isEqualTo(1);
  }
}
