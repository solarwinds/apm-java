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
import static org.junit.jupiter.api.Assertions.assertSame;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TransactionNameManagerTest {

  @AfterEach
  void tearDown() {
    TransactionNameManager.setNamingScheme(new DefaultNamingScheme(null));
  }

  @Test
  void setNamingSchemeIgnoresNull() {
    NamingScheme scheme =
        new SpanAttributeNamingScheme(
            new DefaultNamingScheme(null), ":", Collections.singletonList("HandlerName"));
    TransactionNameManager.setNamingScheme(scheme);

    TransactionNameManager.setNamingScheme(null);
    assertSame(scheme, TransactionNameManager.getNamingScheme());
  }

  @Test
  void buildTransactionNameUsesConfiguredNamingScheme() {
    TransactionNameManager.setNamingScheme(
        new SpanAttributeNamingScheme(
            new DefaultNamingScheme(null), ":", Arrays.asList("HandlerName", "http.method")));

    TestSpanData spanData =
        TestSpanData.builder()
            .setName("test")
            .setKind(SpanKind.SERVER)
            .setStartEpochNanos(0)
            .setEndEpochNanos(10_000_000)
            .setHasEnded(true)
            .setStatus(StatusData.ok())
            .setAttributes(
                Attributes.builder()
                    .put(AttributeKey.stringKey("http.method"), "GET")
                    .put(AttributeKey.stringKey("HandlerName"), "owners")
                    .build())
            .build();

    assertEquals("owners:GET", TransactionNameManager.buildTransactionName(spanData));
  }

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
