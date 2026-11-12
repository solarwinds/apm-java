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

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.Metadata;
import com.solarwinds.joboe.sampling.SamplingException;
import io.opentelemetry.api.trace.SpanContext;

public class Util {
  private static final Logger logger = LoggerFactory.getLogger();
  private static final byte EXIT_OP_ID_MASK = 0xf;

  /**
   * Converts an OpenTelemetry span context to a hex string.
   *
   * @param context otel span context
   * @return span context as hex string
   */
  public static String w3cContextToHexString(SpanContext context) {
    return Metadata.CURRENT_VERSION_HEXSTRING
        + Metadata.HEXSTRING_DELIMETER
        + context.getTraceId()
        + Metadata.HEXSTRING_DELIMETER
        + context.getSpanId()
        + Metadata.HEXSTRING_DELIMETER
        + context.getTraceFlags().asHex();
  }

  /**
   * Builds an AO metadata with OT span context
   *
   * @param context otel span context
   * @return Ao metadata
   */
  public static Metadata buildMetadata(SpanContext context) {
    try {
      return new Metadata(w3cContextToHexString(context));
    } catch (SamplingException e) {
      logger.info(
          "Failed to get AO metadata from span context: " + w3cContextToHexString(context), e);
      return new Metadata();
    }
  }
}
