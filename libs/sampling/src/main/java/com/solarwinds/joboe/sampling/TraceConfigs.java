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

package com.solarwinds.joboe.sampling;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.ToString;

/**
 * Container that stores {@link TraceConfig} mapped by URL
 *
 * @author pluk
 */
@ToString
public class TraceConfigs implements Serializable {
  private static final long serialVersionUID = 1L;

  private final Map<ResourceMatcher, TraceConfig> traceConfigsByMatcher;

  private final Cache<String, TraceConfig> lruCache =
      Caffeine.newBuilder().maximumSize(1048).build();

  private final Cache<String, String> lruCacheKey = Caffeine.newBuilder().maximumSize(1048).build();

  public TraceConfigs(Map<ResourceMatcher, TraceConfig> traceConfigsByMatcher) {
    this.traceConfigsByMatcher = traceConfigsByMatcher;
  }

  public TraceConfig getTraceConfig(List<String> signals) {
    StringBuilder key = new StringBuilder();
    signals.forEach(key::append);
    TraceConfig result = null;

    if (lruCacheKey.getIfPresent(key.toString()) != null) {
      return lruCache.getIfPresent(key.toString());
    }

    outer:
    for (Entry<ResourceMatcher, TraceConfig> entry : traceConfigsByMatcher.entrySet()) {
      for (String signal : signals) {
        if (entry.getKey().matches(signal)) {
          result = entry.getValue();
          break outer;
        }
      }
    }

    if (result != null) {
      lruCache.put(key.toString(), result);
    }

    lruCacheKey.put(key.toString(), key.toString());
    return result;
  }
}
