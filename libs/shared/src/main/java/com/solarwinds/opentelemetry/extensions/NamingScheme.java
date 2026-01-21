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

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import io.opentelemetry.api.common.Attributes;
import java.util.List;

public abstract class NamingScheme {
  protected final NamingScheme next;

  protected NamingScheme(NamingScheme next) {
    this.next = next;
  }

  public abstract String createName(Attributes attributes);

  public static NamingScheme createDecisionChain(List<TransactionNamingScheme> schemes) {
    NamingScheme head = new DefaultNamingScheme(null);
    Logger logger = LoggerFactory.getLogger();

    for (int index = schemes.size() - 1; index > -1; index--) {
      TransactionNamingScheme scheme = schemes.get(index);
      if (scheme == null) {
        logger.debug("Null scheme was encountered. Ensure you don't have any trailing commas");
        continue;
      }

      if (scheme.getDelimiter() != null && scheme.getAttributes() != null) {
        head = new SpanAttributeNamingScheme(head, scheme.getDelimiter(), scheme.getAttributes());
      } else {
        logger.warn(
            String.format(
                "Configured scheme(%s) is missing required fields. Hence, scheme has no effect",
                scheme.getScheme()));
      }
    }

    return head;
  }
}
