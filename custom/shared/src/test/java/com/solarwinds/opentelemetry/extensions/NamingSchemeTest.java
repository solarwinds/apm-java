/*
 * Copyright SolarWinds Worldwide, LLC.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NamingSchemeTest {

  @Captor private ArgumentCaptor<String> stringArgumentCaptor;

  @Mock private Logger loggerMock;

  @Test
  void verifyThatSpanAttributeNamingSchemeIsParsedCorrectly() {
    List<TransactionNamingScheme> schemes =
        Collections.singletonList(
            new TransactionNamingScheme(
                "spanAttribute", ":", Collections.singletonList("http.method")));

    NamingScheme actual = NamingScheme.createDecisionChain(schemes);
    assertNotNull(actual);
    assertTrue(actual instanceof SpanAttributeNamingScheme);
  }

  @Test
  void verifyThatSchemeOrderIsRetained() {
    List<TransactionNamingScheme> schemes =
        Arrays.asList(
            new TransactionNamingScheme(
                "spanAttribute", ":", Collections.singletonList("http.method")),
            new TransactionNamingScheme(
                "spanAttribute", "-", Collections.singletonList("http.method")));

    NamingScheme actual = NamingScheme.createDecisionChain(schemes);
    assertNotNull(actual);

    assertEquals(":", ((SpanAttributeNamingScheme) actual).getDelimiter());
    assertEquals("-", ((SpanAttributeNamingScheme) actual.next).getDelimiter());
  }

  @Test
  void verifyThatNullSchemeIsIgnored() {
    try (MockedStatic<LoggerFactory> loggerFactoryMockedStatic = mockStatic(LoggerFactory.class)) {
      loggerFactoryMockedStatic.when(LoggerFactory::getLogger).thenReturn(loggerMock);
      doNothing().when(loggerMock).debug(any());

      NamingScheme.createDecisionChain(Collections.singletonList(null));

      verify(loggerMock).debug(stringArgumentCaptor.capture());
      assertEquals(
          "Null scheme was encountered. Ensure you don't have any trailing commas",
          stringArgumentCaptor.getValue());
    }
  }
}
