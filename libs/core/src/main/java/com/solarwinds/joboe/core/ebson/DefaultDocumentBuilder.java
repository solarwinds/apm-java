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

package com.solarwinds.joboe.core.ebson;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;

final class DefaultDocumentBuilder implements BsonDocument.Builder {

  private final Map<String, Object> builder;

  DefaultDocumentBuilder() {
    builder = Maps.newLinkedHashMap();
  }

  @Override
  public BsonDocument.Builder putAll(Map<String, Object> map) {
    Preconditions.checkNotNull(map, "null map");
    for (Entry<String, Object> entry : map.entrySet()) put(entry.getKey(), entry.getValue());
    return this;
  }

  @Override
  public BsonDocument.Builder put(String key, @Nullable Object value) {
    Preconditions.checkNotNull(key, "null key");
    Preconditions.checkArgument(!builder.containsKey(key), "key: '%s' is already present", key);
    builder.put(key, value);
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public BsonDocument.Builder putAllowMultiVal(String key, @Nullable Object value) {
    Preconditions.checkNotNull(key, "null key");
    if (builder.containsKey(key)) {
      Object existingValue = builder.get(key);
      MultiValList<Object> list;
      if (existingValue instanceof MultiValList) {
        list = (MultiValList<Object>) existingValue;
      } else {
        // convert the existing value into a MultiValList
        list = new MultiValList<Object>();
        list.add(existingValue);
        builder.put(key, list);
      }
      list.add(value);
    } else {
      builder.put(key, value);
    }
    return this;
  }

  @Override
  public BsonDocument build() {
    return new DefaultDocument(builder.isEmpty() ? Collections.emptyMap() : builder);
  }
}
