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

package com.solarwinds.opentelemetry.extensions.provider;

import static com.solarwinds.opentelemetry.extensions.ApmResourceProvider.moduleKey;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.solarwinds.opentelemetry.extensions.config.provider.ResourceComponentProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceComponentProviderTest {

  private final ResourceComponentProvider tested = new ResourceComponentProvider();

  @Mock private DeclarativeConfigProperties declarativeConfigPropertiesMock;

  @Test
  void testName() {
    assertEquals("swo/resource", tested.getName());
  }

  @Test
  void getResource() {
    Resource resource = tested.create(declarativeConfigPropertiesMock);
    String attribute = resource.getAttribute(moduleKey);
    assertEquals("apm", attribute);
  }
}
