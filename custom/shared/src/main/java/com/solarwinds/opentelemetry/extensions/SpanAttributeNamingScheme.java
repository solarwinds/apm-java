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

package com.solarwinds.opentelemetry.extensions;

import io.opentelemetry.api.common.Attributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class SpanAttributeNamingScheme extends NamingScheme {
  String delimiter;
  List<String> keys;

  public SpanAttributeNamingScheme(NamingScheme next, String delimiter, List<String> keys) {
    super(next);
    this.delimiter = delimiter;
    this.keys = keys;
  }

  @Override
  public String createName(Attributes attributes) {
    List<String> items = new ArrayList<>();
    // We expect the string output to be in the same order as `keys`
    // so we build an intermediary map
    Map<String, Object> kvs = new HashMap<>();
    // The Attributes interface requires a type for each get call
    // `get(AttributeKey<T> key)` and we need to do it across multiple
    // keys, so we simply iterate over all attributes.
    attributes.forEach(
        (k, v) -> {
          if (keys.contains(k.getKey())) {
            kvs.put(k.getKey(), v);
          }
        });

    // If we extracted nothing, pass it down the chain
    if (kvs.isEmpty()) {
      return next.createName(attributes);
    }

    // Now iterate over the expected keys to maintain order
    for (String key : keys) {
      Object v = kvs.get(key);
      if (v == null) {
        continue;
      }
      if (v instanceof List<?>) {
        List<?> list = (List<?>) v;
        for (Object o : list) {
          items.add(String.valueOf(o));
        }
      } else {
        items.add(String.valueOf(v));
      }
    }
    return String.join(delimiter, items);
  }
}
