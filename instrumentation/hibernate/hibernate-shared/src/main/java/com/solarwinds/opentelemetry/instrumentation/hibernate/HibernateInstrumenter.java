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

package com.solarwinds.opentelemetry.instrumentation.hibernate;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;

public final class HibernateInstrumenter {
  private static final Instrumenter<String, Void> INSTANCE =
      Instrumenter.<String, Void>builder(
              GlobalOpenTelemetry.get(), "com.solarwinds.hibernate-6.0", (sql) -> "sw.jdbc.context")
          .addAttributesExtractor(new SqlAttributeExtractor())
          .buildInstrumenter();

  public static Instrumenter<String, Void> getInstance() {
    return INSTANCE;
  }
}
