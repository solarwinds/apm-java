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

import com.solarwinds.joboe.sampling.ResourceMatcher;
import java.util.regex.Pattern;

public class StringPatternMatcher implements ResourceMatcher {
  private final Pattern pattern;

  public StringPatternMatcher(Pattern pattern) {
    this.pattern = pattern;
  }

  @Override
  public boolean matches(String resource) {
    return pattern.matcher(resource).matches();
  }
}
