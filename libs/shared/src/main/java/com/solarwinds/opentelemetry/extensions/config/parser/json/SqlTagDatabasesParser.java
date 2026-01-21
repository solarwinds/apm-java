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

package com.solarwinds.opentelemetry.extensions.config.parser.json;

import com.solarwinds.joboe.config.ConfigParser;
import com.solarwinds.joboe.config.InvalidConfigException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class SqlTagDatabasesParser implements ConfigParser<String, Set<String>> {
  @Override
  public Set<String> convert(String databases) throws InvalidConfigException {
    return Arrays.stream(databases.split(",")).map(String::toLowerCase).collect(Collectors.toSet());
  }
}
