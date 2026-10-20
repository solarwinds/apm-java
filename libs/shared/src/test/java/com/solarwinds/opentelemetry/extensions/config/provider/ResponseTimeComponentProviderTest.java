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

package com.solarwinds.opentelemetry.extensions.config.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.opentelemetry.extensions.DefaultNamingScheme;
import com.solarwinds.opentelemetry.extensions.NamingScheme;
import com.solarwinds.opentelemetry.extensions.SpanAttributeNamingScheme;
import com.solarwinds.opentelemetry.extensions.TransactionNameManager;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ResponseTimeComponentProviderTest {
  private final ResponseTimeComponentProvider tested = new ResponseTimeComponentProvider();

  @AfterEach
  void tearDown() {
    ConfigManager.reset();
    TransactionNameManager.setNamingScheme(new DefaultNamingScheme(null));
  }

  @Test
  void testName() {
    assertEquals("swo_response_time", tested.getName());
  }

  @Test
  void testCreateAppliesTransactionName() {
    SpanProcessor processor = tested.create(loadConfig());

    assertNotNull(processor);
    assertEquals(
        "custom-name",
        ConfigManager.getConfigOptional(ConfigProperty.AGENT_TRANSACTION_NAME, null));
  }

  @Test
  void testCreateWithoutTransactionName() {
    SpanProcessor processor = tested.create(DeclarativeConfigProperties.empty());

    assertNotNull(processor);
    assertNull(ConfigManager.getConfigOptional(ConfigProperty.AGENT_TRANSACTION_NAME, null));
  }

  @Test
  void testCreateAppliesTransactionNameSchemes() {
    tested.create(loadConfig());
    NamingScheme expected =
        new SpanAttributeNamingScheme(
            new DefaultNamingScheme(null), ":", Collections.singletonList("HandlerName"));

    assertEquals(expected, TransactionNameManager.getNamingScheme());
  }

  @Test
  void testCreateIgnoresEmptyTransactionName() {
    tested.create(fromYaml("swo_response_time:\n  transaction_name: \"\"\n"));
    assertNull(ConfigManager.getConfigOptional(ConfigProperty.AGENT_TRANSACTION_NAME, null));
  }

  @Test
  void testCreateKeepsDefaultSchemeWhenNoSchemesConfigured() {
    NamingScheme before = TransactionNameManager.getNamingScheme();

    tested.create(fromYaml("swo_response_time:\n  transaction_name: name\n"));
    assertSame(before, TransactionNameManager.getNamingScheme());
  }

  @Test
  void testCreateChainsSchemesInPriorityOrder() {
    tested.create(
        fromYaml(
            "swo_response_time:\n"
                + "  transaction_name_schemes:\n"
                + "    - scheme: spanAttribute\n"
                + "      delimiter: ':'\n"
                + "      attributes: [first.a, first.b]\n"
                + "    - scheme: spanAttribute\n"
                + "      delimiter: '-'\n"
                + "      attributes: [second.a, second.b]\n"));
    NamingScheme scheme = TransactionNameManager.getNamingScheme();

    Attributes both =
        Attributes.builder()
            .put(AttributeKey.stringKey("first.b"), "fb")
            .put(AttributeKey.stringKey("first.a"), "fa")
            .put(AttributeKey.stringKey("second.a"), "sa")
            .build();

    Attributes onlySecond =
        Attributes.builder()
            .put(AttributeKey.stringKey("second.b"), "sb")
            .put(AttributeKey.stringKey("second.a"), "sa")
            .build();

    assertEquals("fa:fb", scheme.createName(both));
    assertEquals("sa-sb", scheme.createName(onlySecond));
  }

  private DeclarativeConfigProperties fromYaml(String yaml) {
    return DeclarativeConfiguration.toConfigProperties(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
        .getStructured("swo_response_time");
  }

  private DeclarativeConfigProperties loadConfig() {
    try (InputStream in = getClass().getResourceAsStream("/sdk-config.yaml")) {
      return DeclarativeConfiguration.toConfigProperties(in)
          .getStructured("tracer_provider", DeclarativeConfigProperties.empty())
          .getStructuredList("processors", Collections.emptyList())
          .stream()
          .map(processor -> processor.getStructured("swo_response_time"))
          .filter(Objects::nonNull)
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("swo_response_time not found"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
