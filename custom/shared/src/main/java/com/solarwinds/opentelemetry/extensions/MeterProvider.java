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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.Meter;

public class MeterProvider {

  public static final String samplingMeterScopeName = "sw.apm.sampling.metrics";

  public static final String requestMeterScopeName = "sw.apm.request.metrics";

  public static Meter getSamplingMetricsMeter() {
    return GlobalOpenTelemetry.meterBuilder(samplingMeterScopeName)
        .setInstrumentationVersion(BuildConfig.SOLARWINDS_AGENT_VERSION)
        .build();
  }

  public static Meter getRequestMetricsMeter() {
    return GlobalOpenTelemetry.meterBuilder(requestMeterScopeName)
        .setInstrumentationVersion(BuildConfig.SOLARWINDS_AGENT_VERSION)
        .build();
  }
}
