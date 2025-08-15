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

package com.solarwinds.opentelemetry.extensions.config.parser.yaml;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.ConfigParser;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.sampling.TracingMode;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;

@SuppressWarnings("rawtypes")
@AutoService(ConfigParser.class)
public class TracingModeParser implements ConfigParser<DeclarativeConfigProperties, TracingMode> {
  private static final String CONFIG_KEY = "agent.tracingMode";

  @Override
  public TracingMode convert(DeclarativeConfigProperties declarativeConfigProperties)
      throws InvalidConfigException {
    String mode = declarativeConfigProperties.getString(CONFIG_KEY);
    if (mode == null) {
      return null;
    }

    TracingMode tracingMode = TracingMode.fromString(mode);
    if (tracingMode != null) {
      return tracingMode;
    } else {
      throw new InvalidConfigException(
          "Invalid "
              + ConfigProperty.AGENT_TRACING_MODE.getConfigFileKey()
              + " : "
              + mode
              + ", must be \"disabled\" or \"enabled\"");
    }
  }

  @Override
  public String configKey() {
    return CONFIG_KEY;
  }
}
