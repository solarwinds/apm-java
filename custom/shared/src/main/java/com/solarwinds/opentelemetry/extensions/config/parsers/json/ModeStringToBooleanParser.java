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

package com.solarwinds.opentelemetry.extensions.config.parsers.json;

import com.solarwinds.joboe.config.ConfigParser;
import com.solarwinds.joboe.config.InvalidConfigException;

public class ModeStringToBooleanParser implements ConfigParser<String, Boolean> {
  public static final ModeStringToBooleanParser INSTANCE = new ModeStringToBooleanParser();

  private ModeStringToBooleanParser() {}

  @Override
  public Boolean convert(String input) throws InvalidConfigException {
    if ("enabled".equals(input)) {
      return true;
    } else if ("disabled".equals(input)) {
      return false;
    } else {
      throw new InvalidConfigException(
          "Expected value [enabled] or [disabled] but found [" + input + "]");
    }
  }
}
