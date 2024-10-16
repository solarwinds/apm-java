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

package com.solarwinds.opentelemetry.instrumentation.hibernate.v6_0;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.semconv.SemanticAttributes;
import javax.annotation.Nullable;

public class SqlAttributeExtractor implements AttributesExtractor<String, Void> {
  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, String sql) {
    attributes.put(SemanticAttributes.DB_STATEMENT, sql);
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      String sql,
      @Nullable Void v,
      @Nullable Throwable error) {}
}
