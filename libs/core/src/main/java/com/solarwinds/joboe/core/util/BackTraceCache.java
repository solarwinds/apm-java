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

package com.solarwinds.joboe.core.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BackTraceCache {
  private static Cache<List<StackTraceElement>, String> backTraceCache =
      null; // delay building cache as it causes problem in jboss see
  // https://github.com/librato/joboe/issues/594
  private static volatile boolean enabled = false;

  static String getBackTraceString(List<StackTraceElement> stackTrace) {
    Cache<List<StackTraceElement>, String> cache = getCache();
    return cache != null ? cache.getIfPresent(stackTrace) : null;
  }

  static void putBackTraceString(List<StackTraceElement> stackTrace, String stackTraceString) {
    Cache<List<StackTraceElement>, String> cache = getCache();
    if (cache != null) {
      cache.put(stackTrace, stackTraceString);
    }
  }

  public static final void enable() {
    enabled = true;
  }

  private static Cache<List<StackTraceElement>, String> getCache() {
    if (backTraceCache == null && enabled) {
      backTraceCache =
          CacheBuilder.newBuilder()
              .maximumSize(20)
              .expireAfterAccess(3600, TimeUnit.SECONDS)
              .build(); // 1 hour cache
    }

    return backTraceCache;
  }
}
