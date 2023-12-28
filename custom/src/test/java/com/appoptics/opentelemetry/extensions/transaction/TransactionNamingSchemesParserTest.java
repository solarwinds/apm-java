package com.appoptics.opentelemetry.extensions.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.solarwinds.joboe.core.config.InvalidConfigException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransactionNamingSchemesParserTest {

  private final TransactionNamingSchemesParser tested = new TransactionNamingSchemesParser();

  @Test
  void testConvert() throws InvalidConfigException {
    String json =
        "  [\n"
            + "   {\n"
            + "      \"scheme\": \"spanAttribute\",\n"
            + "      \"delimiter\": \"-\",\n"
            + "      \"attributes\": [\"http.method\"]\n"
            + "    },\n"
            + "    {\n"
            + "      \"scheme\": \"spanAttribute\",\n"
            + "      \"delimiter\": \":\",\n"
            + "      \"attributes\": [\"http.route\",\"HandlerName\"]\n"
            + "    }\n"
            + "  ]\n";

    List<TransactionNamingScheme> actual = tested.convert(json);
    List<TransactionNamingScheme> expected =
        Arrays.asList(
            new TransactionNamingScheme(
                "spanAttribute", "-", Collections.singletonList("http.method")),
            new TransactionNamingScheme(
                "spanAttribute", ":", Arrays.asList("http.route", "HandlerName")));

    assertEquals(expected, actual);
  }
}
