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

import com.solarwinds.api.ext.SolarwindsAgent;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class TransactionNamingTest {

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Test
  void verifySdkSetTransactionName() {
    testing.runWithSpan(
        "root",
        () -> {
          SolarwindsAgent.setTransactionName("int-test");
        });

    List<List<SpanData>> traces = testing.waitForTraces(1);
    assertThat(traces).hasSize(1);

    SpanData rootSpan = traces.get(0).get(0);
    assertThat(rootSpan.getAttributes().get(AttributeKey.stringKey("sw.transaction")))
        .isEqualTo("int-test");
  }

  @Test
  void verifyTransactionNameFallsBackToSpanName() {
    testing.runWithSpan("GET /petclinic/api/owners", () -> {});

    List<List<SpanData>> traces = testing.waitForTraces(1);
    assertThat(traces).hasSize(1);

    SpanData rootSpan = traces.get(0).get(0);
    String transactionName = rootSpan.getAttributes().get(AttributeKey.stringKey("sw.transaction"));
    assertThat(transactionName).isEqualTo("GET /petclinic/api/owners");
  }
}
