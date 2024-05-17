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

package com.solarwinds.opentelemetry.core;

public class Constants {
  // FIXME: bad practice to define a collection of constants in either a class or an interface (even
  // worse)
  public static final String SW_KEY_PREFIX = "sw.";
  public static final String OT_KEY_PREFIX = "ot.";
  public static final String SW_INTERNAL_ATTRIBUTE_PREFIX = SW_KEY_PREFIX + "internal.";
  public static final String SW_DETAILED_TRACING = SW_INTERNAL_ATTRIBUTE_PREFIX + "detailedTracing";
  public static final String SW_METRICS = SW_INTERNAL_ATTRIBUTE_PREFIX + "metrics";
  public static final String SW_SAMPLER = SW_INTERNAL_ATTRIBUTE_PREFIX + "sampler";
  public static final String W3C_KEY_PREFIX = "w3c.";
  public static final String SW_UPSTREAM_TRACESTATE = SW_KEY_PREFIX + W3C_KEY_PREFIX + "tracestate";
  public static final String SW_PARENT_ID = SW_KEY_PREFIX + "tracestate_parent_id";
}
