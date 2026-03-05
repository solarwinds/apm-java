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

package com.solarwinds.joboe.core;

/**
 * Known X-Trace headers used by our Agent. Take note that these headers are not tied to any
 * protocol (Http for instance). It is the caller who should map the input headers to the
 * corresponding X-Trace headers of this enum
 *
 * @author Patson Luk
 */
public enum XTraceHeader {
  TRACE_ID,
  SPAN_ID,
  TRACE_OPTIONS,
  TRACE_OPTIONS_SIGNATURE
}
