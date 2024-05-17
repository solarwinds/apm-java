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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;

public class CustomTransactionNameDict {
  private static final Cache<String, String> dict =
      Caffeine.newBuilder()
          .maximumSize(1000)
          .expireAfterAccess(Duration.ofMinutes(20))
          .<String, String>build(); // 20 mins cache

  public static void set(String id, String name) {
    dict.put(id, name);
  }

  public static String get(String id) {
    return dict.getIfPresent(id);
  }
}
