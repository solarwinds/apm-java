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

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SpanStacktraceFilter implements Predicate<ReadableSpan> {
  private Set<String> filterAttributes = new HashSet<>();

  @Override
  public boolean test(ReadableSpan readableSpan) {
    if (filterAttributes.isEmpty()) {
      Set<String> configuredFilterAttributes =
          ConfigManager.getConfigOptional(
              ConfigProperty.AGENT_SPAN_STACKTRACE_FILTERS, Collections.singleton("db.system"));

      if (configuredFilterAttributes.size() > 1) {
        configuredFilterAttributes.add("db.system");
      }
      filterAttributes = configuredFilterAttributes;
    }

    Set<String> attributes =
        readableSpan.getAttributes().asMap().keySet().stream()
            .map(AttributeKey::getKey)
            .collect(Collectors.toSet());
    attributes.retainAll(filterAttributes);
    return !attributes.isEmpty();
  }
}
