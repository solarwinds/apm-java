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

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.opentelemetry.extensions.NamingScheme;
import com.solarwinds.opentelemetry.extensions.ResponseTimeSpanProcessor;
import com.solarwinds.opentelemetry.extensions.TransactionNameManager;
import com.solarwinds.opentelemetry.extensions.TransactionNamingScheme;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@AutoService(ComponentProvider.class)
public class ResponseTimeComponentProvider implements ComponentProvider {

  public static final String COMPONENT_NAME = "swo_response_time";

  @Override
  public Class<SpanProcessor> getType() {
    return SpanProcessor.class;
  }

  @Override
  public String getName() {
    return COMPONENT_NAME;
  }

  @Override
  public SpanProcessor create(DeclarativeConfigProperties declarativeConfigProperties) {
    String transactionName = declarativeConfigProperties.getString("transaction_name");
    if (transactionName != null && !transactionName.isEmpty()) {
      try {
        ConfigManager.setConfig(ConfigProperty.AGENT_TRANSACTION_NAME, transactionName);
      } catch (InvalidConfigException e) {
        throw new RuntimeException(e);
      }
    }

    List<TransactionNamingScheme> schemes =
        declarativeConfigProperties
            .getStructuredList("transaction_name_schemes", Collections.emptyList())
            .stream()
            .map(this::toScheme)
            .collect(Collectors.toList());

    if (!schemes.isEmpty()) {
      TransactionNameManager.setNamingScheme(NamingScheme.createDecisionChain(schemes));
    }

    return new ResponseTimeSpanProcessor();
  }

  private TransactionNamingScheme toScheme(DeclarativeConfigProperties properties) {
    return new TransactionNamingScheme(
        properties.getString("scheme"),
        properties.getString("delimiter"),
        properties.getScalarList("attributes", String.class, Collections.emptyList()));
  }
}
