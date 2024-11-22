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

import static com.solarwinds.opentelemetry.extensions.ApmResourceProvider.moduleKey;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApmResourceProviderTest {
  @InjectMocks private ApmResourceProvider apmResourceProvider;

  @Test
  void testCreateResource() {
    Resource resource =
        apmResourceProvider.createResource(DefaultConfigProperties.create(Collections.emptyMap()));
    String module = resource.getAttribute(moduleKey);
    String version = resource.getAttribute(moduleKey);

    assertNotNull(module);
    assertNotNull(version);
  }
}
