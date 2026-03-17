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

package com.solarwinds.joboe.config;

public interface ConfigParser<T, R> {
  /**
   * Convert the input of type T into result of R. If there are any error during the conversion, it
   * should throw InvalidConfigException
   *
   * @param input
   * @return
   * @throws InvalidConfigException
   */
  R convert(T input) throws InvalidConfigException;

  default String configKey() {
    return "";
  }
}
