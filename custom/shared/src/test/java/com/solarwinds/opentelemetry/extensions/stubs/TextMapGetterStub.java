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

package com.solarwinds.opentelemetry.extensions.stubs;

import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.Map;

@SuppressWarnings("all")
public class TextMapGetterStub implements TextMapGetter<Map<String, String>> {
  @Override
  public Iterable<String> keys(Map<String, String> carrier) {
    return carrier.keySet();
  }

  @Override
  public String get(Map<String, String> carrier, String key) {
    assert carrier != null;
    return carrier.get(key);
  }
}