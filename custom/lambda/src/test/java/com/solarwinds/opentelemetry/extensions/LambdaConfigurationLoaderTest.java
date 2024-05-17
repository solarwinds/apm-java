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

import com.solarwinds.joboe.config.ConfigContainer;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LambdaConfigurationLoaderTest {
  @InjectMocks private LambdaConfigurationLoader tested;

  @Test
  void processConfigs() throws InvalidConfigException {
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

    ConfigContainer configContainer = new ConfigContainer();
    configContainer.putByStringValue(ConfigProperty.AGENT_TRANSACTION_NAMING_SCHEMES, json);

    LambdaConfigurationLoader.processConfigs(configContainer);
    NamingScheme expected =
        new SpanAttributeNamingScheme(
            new SpanAttributeNamingScheme(
                new DefaultNamingScheme(null), ":", Arrays.asList("http.route", "HandlerName")),
            "-",
            Collections.singletonList("http.method"));

    assertEquals(expected, TransactionNameManager.getNamingScheme());
  }
}
