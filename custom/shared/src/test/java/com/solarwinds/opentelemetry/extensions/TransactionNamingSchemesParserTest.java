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

import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.opentelemetry.extensions.config.parsers.json.TransactionNamingSchemesParser;
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
