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

package com.solarwinds.util;

/** An container to hold both the local and container naming conventions. */
public class NamingConventions {

  public final NamingConvention container = new NamingConvention("/results");
  public final NamingConvention local = new NamingConvention("./k6-results");

  /** @return Root path for the local naming convention (where results are output) */
  public String localResults() {
    return local.root();
  }

  /** @return Root path for the container naming convention (where results are output) */
  public String containerResults() {
    return container.root();
  }
}
