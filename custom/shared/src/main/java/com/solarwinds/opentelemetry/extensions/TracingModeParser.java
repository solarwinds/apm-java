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

import com.solarwinds.joboe.config.ConfigParser;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.sampling.TracingMode;

public class TracingModeParser implements ConfigParser<String, TracingMode> {

  @Override
  public TracingMode convert(String argVal) throws InvalidConfigException {
    if (argVal != null) {
      TracingMode tracingMode = TracingMode.fromString(argVal);
      if (tracingMode != null) {
        return tracingMode;
      } else {
        throw new InvalidConfigException(
            "Invalid "
                + ConfigProperty.AGENT_TRACING_MODE.getConfigFileKey()
                + " : "
                + argVal
                + ", must be \"disabled\" or \"enabled\"");
      }
    } else {
      return null;
    }
  }
}
