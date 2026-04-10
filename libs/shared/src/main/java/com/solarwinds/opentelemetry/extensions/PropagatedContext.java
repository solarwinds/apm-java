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

import com.solarwinds.joboe.sampling.XtraceOptions;

/**
 * Thread-local workaround for OTel SDK replacing the parent context with a primordial context for
 * root spans (see open-telemetry/opentelemetry-java#8012). Remove once the upstream fix is shipped.
 */
final class PropagatedContext {
  private static final ThreadLocal<XtraceOptions> XTRACE_OPTIONS = new ThreadLocal<>();

  private PropagatedContext() {}

  static void set(XtraceOptions options) {
    XTRACE_OPTIONS.set(options);
  }

  static XtraceOptions getXtraceOptions() {
    return XTRACE_OPTIONS.get();
  }

  static void clear() {
    XTRACE_OPTIONS.remove();
  }
}
