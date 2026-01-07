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

import com.solarwinds.joboe.sampling.ResourceMatcher;
import java.util.Set;

public class ResourceExtensionsMatcher implements ResourceMatcher {
  private final Set<String> extensions;

  public ResourceExtensionsMatcher(Set<String> extensions) {
    super();
    this.extensions = extensions;
  }

  @Override
  public boolean matches(String resource) {
    int queryIndex = resource.indexOf('?');
    resource =
        queryIndex != -1
            ? resource.substring(0, queryIndex)
            : resource; // remove query component if present
    int extensionIndex = resource.lastIndexOf('.');
    String extension =
        extensionIndex != -1 ? resource.substring(extensionIndex + 1).toLowerCase() : null;

    return extension != null && extensions.contains(extension);
  }
}
