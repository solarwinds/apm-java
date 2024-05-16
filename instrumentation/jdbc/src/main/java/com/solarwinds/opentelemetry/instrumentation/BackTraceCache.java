/*
 * Copyright SolarWinds Worldwide, LLC.
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

package com.solarwinds.opentelemetry.instrumentation;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;

public class BackTraceCache {
  private static final Cache<List<StackTraceElement>, String> backTraceCache =
      Caffeine.newBuilder()
          .maximumSize(20)
          .expireAfterAccess(Duration.ofHours(1L))
          .build(); // 1 hour cache;

  static String getBackTraceString(List<StackTraceElement> stackTrace) {
    return backTraceCache.getIfPresent(stackTrace);
  }

  static void putBackTraceString(List<StackTraceElement> stackTrace, String stackTraceString) {
    backTraceCache.put(stackTrace, stackTraceString);
  }
}
