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

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ResponseTimeComponentProviderTest {
  private final ResponseTimeComponentProvider tested = new ResponseTimeComponentProvider();

  @AfterEach
  void tearDown() {
    ConfigManager.reset();
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
