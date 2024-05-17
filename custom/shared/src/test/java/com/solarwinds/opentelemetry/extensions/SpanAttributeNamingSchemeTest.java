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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.SemanticAttributes;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpanAttributeNamingSchemeTest {
  private SpanAttributeNamingScheme tested;

  @Mock private SpanData spanDataMock;

  @Mock private NamingScheme namingSchemeMock;

  @BeforeEach
  void setup() {
    tested =
        new SpanAttributeNamingScheme(
            namingSchemeMock, "-", Arrays.asList("http.request.method", "Handler", "key-name"));
  }

  @Test
  void verifyThatNameIsReturnedWhenOneAttributeExists() {
    String name = tested.createName(Attributes.of(SemanticAttributes.HTTP_REQUEST_METHOD, "POST"));
    assertEquals("POST", name);
  }

  @Test
  void verifyThatNameIsReturnedWhenMoreThanOneAttributesExist() {
    Attributes attributes =
        Attributes.builder()
            .put(SemanticAttributes.HTTP_REQUEST_METHOD, "POST")
            .put(AttributeKey.stringKey("Handler"), "Controller.segfault")
            .build();

    String name = tested.createName(attributes);
    assertEquals("POST-Controller.segfault", name);
  }

  @Test
  void verifyThatNameIsReturnedWhenAttributeValueIsList() {
    Attributes attributes =
        Attributes.builder()
            .put(
                AttributeKey.stringArrayKey("Handler"),
                Arrays.asList("POST", "Controller.segfault"))
            .build();

    String name = tested.createName(attributes);
    assertEquals("POST-Controller.segfault", name);
  }

  @Test
  void verifyDelegationToNextWhenNoMatchingAttributeExist() {
    when(namingSchemeMock.createName(any())).thenReturn("Hello");
    String name = tested.createName(Attributes.empty());
    assertEquals("Hello", name);
  }

  @Test
  void returnStringBuilderValueGivenBooleanValue() {
    String attributeKey = "key-name";
    Attributes attributes = Attributes.of(AttributeKey.booleanKey(attributeKey), false);

    String actual = tested.createName(attributes);
    assertEquals("false", actual);
  }

  @Test
  void returnStringBuilderValueGivenBooleanListValue() {
    String attributeKey = "key-name";
    Attributes attributes =
        Attributes.of(AttributeKey.booleanArrayKey(attributeKey), Arrays.asList(false, true));

    String actual = tested.createName(attributes);
    assertEquals("false-true", actual);
  }

  @Test
  void returnStringBuilderValueGivenDoubleListValue() {
    String attributeKey = "key-name";
    Attributes attributes =
        Attributes.of(AttributeKey.doubleArrayKey(attributeKey), Arrays.asList(23.0, 0.0));

    String actual = tested.createName(attributes);
    assertEquals("23.0-0.0", actual);
  }

  @Test
  void returnStringBuilderValueGivenDoubleValue() {
    String attributeKey = "key-name";
    Attributes attributes = Attributes.of(AttributeKey.doubleKey(attributeKey), 233.98);

    String actual = tested.createName(attributes);
    assertEquals("233.98", actual);
  }

  @Test
  void returnStringBuilderValueGivenLongListValue() {
    String attributeKey = "key-name";
    Attributes attributes =
        Attributes.of(AttributeKey.longArrayKey(attributeKey), Arrays.asList(23L, 0L));

    String actual = tested.createName(attributes);
    assertEquals("23-0", actual);
  }

  @Test
  void returnStringBuilderValueGivenLongValue() {
    String attributeKey = "key-name";
    Attributes attributes = Attributes.of(AttributeKey.longKey(attributeKey), 230L);

    String actual = tested.createName(attributes);
    assertEquals("230", actual);
  }
}
