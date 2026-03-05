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

package com.solarwinds.joboe.sampling;

import lombok.Getter;

/**
 * Contains the result of trace decision (whether a request is sampled and whether metrics should be
 * reported)
 *
 * <p>Also contains the {@link TraceConfig} used for the trace decision-making logic, this could be
 * null if no config was found
 *
 * @author pluk
 */
@Getter
public class TraceDecision {
  private final boolean sampled;
  private final boolean reportMetrics;
  private final TraceConfig traceConfig;
  private final boolean bucketExhausted;
  private final TraceDecisionUtil.RequestType requestType;
  private final Metadata incomingMetadata;

  public TraceDecision(boolean sampled, boolean reportMetrics, TraceConfig traceConfig) {
    this(sampled, reportMetrics, false, traceConfig, TraceDecisionUtil.RequestType.REGULAR);
  }

  public TraceDecision(
      boolean sampled,
      boolean reportMetrics,
      TraceConfig traceConfig,
      TraceDecisionUtil.RequestType requestType) {
    this(sampled, reportMetrics, false, traceConfig, requestType);
  }

  public TraceDecision(
      boolean sampled,
      boolean reportMetrics,
      boolean bucketExhausted,
      TraceConfig traceConfig,
      TraceDecisionUtil.RequestType requestType) {
    this(sampled, reportMetrics, bucketExhausted, traceConfig, requestType, null);
  }

  public TraceDecision(
      boolean sampled,
      boolean reportMetrics,
      boolean bucketExhausted,
      TraceConfig traceConfig,
      TraceDecisionUtil.RequestType requestType,
      Metadata incomingMetadata) {
    super();
    this.sampled = sampled;
    this.reportMetrics = reportMetrics;
    this.bucketExhausted = bucketExhausted;
    this.traceConfig = traceConfig;
    this.requestType = requestType;
    this.incomingMetadata = incomingMetadata;
  }
}
