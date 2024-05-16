/*
 * Copyright SolarWinds Worldwide, LLC.
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

import com.solarwinds.joboe.sampling.XTraceOptions;
import io.opentelemetry.context.ContextKey;

final class TriggerTraceContextKey {
  public static final ContextKey<XTraceOptions> KEY = ContextKey.named("sw-trigger-trace-key");
  public static final ContextKey<String> XTRACE_OPTIONS = ContextKey.named("xtrace-options");
  public static final ContextKey<String> XTRACE_OPTIONS_SIGNATURE =
      ContextKey.named("xtrace-options-signature");

  private TriggerTraceContextKey() {}
}
