package com.solarwinds.opentelemetry.extensions.transaction;

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
