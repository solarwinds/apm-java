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

package com.solarwinds.opentelemetry.extensions.config.parsers.yaml;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.ConfigParser;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.opentelemetry.extensions.TransactionNamingScheme;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@AutoService(ConfigParser.class)
public final class TransactionNamingSchemesParser
    implements ConfigParser<DeclarativeConfigProperties, List<TransactionNamingScheme>> {
  private static final String CONFIG_KEY = "agent.transactionNameSchemes";

  @Override
  public List<TransactionNamingScheme> convert(
      DeclarativeConfigProperties declarativeConfigProperties) throws InvalidConfigException {
    List<DeclarativeConfigProperties> schemes =
        declarativeConfigProperties.getStructuredList(CONFIG_KEY, Collections.emptyList());
    return schemes.stream().map(this::toScheme).collect(Collectors.toList());
  }

  private TransactionNamingScheme toScheme(
      DeclarativeConfigProperties declarativeConfigProperties) {
    String scheme = declarativeConfigProperties.getString("scheme");
    String delimiter = declarativeConfigProperties.getString("delimiter");
    List<String> attributes =
        declarativeConfigProperties.getScalarList(
            "attributes", String.class, Collections.emptyList());

    return new TransactionNamingScheme(scheme, delimiter, attributes);
  }

  @Override
  public String configKey() {
    return CONFIG_KEY;
  }
}
