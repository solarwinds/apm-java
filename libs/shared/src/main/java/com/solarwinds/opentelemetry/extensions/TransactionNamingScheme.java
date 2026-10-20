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

import java.util.List;
import lombok.Value;

/**
 * A user-configured rule for deriving a transaction name from span attributes. Schemes are listed
 * in priority order under {@code transaction_name_schemes} of the {@code swo_response_time}
 * component and chained by {@link NamingScheme#createDecisionChain(List)}; the first scheme whose
 * attributes are present on the span determines the name.
 */
@Value
public class TransactionNamingScheme {
  /** The scheme type, e.g. {@code spanAttribute}. Used for diagnostics only. */
  String scheme;

  /** Separator placed between attribute values when composing the transaction name. */
  String delimiter;

  /** Span attribute keys whose values make up the transaction name, in output order. */
  List<String> attributes;
}
