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

public final class SharedNames {
  private SharedNames() {}

  public static String COMPONENT_NAME = "solarwinds";

  public static String TRANSACTION_NAME_KEY = "sw.transaction";

  public static String SPAN_STACKTRACE_FILTER_CLASS =
      "com.solarwinds.opentelemetry.extensions.SpanStacktraceFilter";

  // This is visible to customer via span layer and can be used to configure transaction
  // filtering setting.
  public static final String LAYER_NAME_PLACEHOLDER = "%s:%s";
}
