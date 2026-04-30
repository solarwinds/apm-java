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

import static com.solarwinds.opentelemetry.extensions.SharedNames.TRANSACTION_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import org.junit.jupiter.api.Test;

class TransactionNameManagerTest {

  @Test
  void buildTransactionNameReturnsPreComputedNameFromSpanAttribute() {
    TestSpanData spanData =
        TestSpanData.builder()
            .setName("test")
            .setKind(SpanKind.SERVER)
            .setStartEpochNanos(0)
            .setEndEpochNanos(10_000_000)
            .setHasEnded(true)
            .setStatus(StatusData.ok())
            .setAttributes(Attributes.of(TRANSACTION_NAME_KEY, "pre-computed-name"))
            .build();

    String result = TransactionNameManager.buildTransactionName(spanData);
    assertEquals("pre-computed-name", result);
  }

  @Test
  void buildTransactionNameFallsBackWhenPreComputedNameIsEmpty() {
    TestSpanData spanData =
        TestSpanData.builder()
            .setName("test")
            .setKind(SpanKind.SERVER)
            .setStartEpochNanos(0)
            .setEndEpochNanos(10_000_000)
            .setHasEnded(true)
            .setStatus(StatusData.ok())
            .setAttributes(Attributes.of(TRANSACTION_NAME_KEY, ""))
            .build();

    String result = TransactionNameManager.buildTransactionName(spanData);
    assertNotEquals("", result);
  }

  @Test
  void buildTransactionNameFallsBackWhenTransactionNameAttributeIsAbsent() {
    TestSpanData spanData =
        TestSpanData.builder()
            .setName("test")
            .setKind(SpanKind.SERVER)
            .setStartEpochNanos(0)
            .setEndEpochNanos(10_000_000)
            .setHasEnded(true)
            .setStatus(StatusData.ok())
            .setAttributes(Attributes.empty())
            .build();

    String result = TransactionNameManager.buildTransactionName(spanData);
    assertNotNull(result);
    assertNotEquals("", result);
  }
}
