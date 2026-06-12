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

package com.solarwinds.joboe.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

class AzureResourceReaderTest {

  @Test
  @ClearEnvironmentVariable(key = "CONTAINER_APP_NAME")
  @ClearEnvironmentVariable(key = "WEBSITE_SITE_NAME")
  @ClearEnvironmentVariable(key = "FUNCTIONS_EXTENSION_VERSION")
  void detectPlatformreturnsNonewhenNoAzureEnvVarsPresent() {
    assertEquals("NONE", AzureResourceReader.detectPlatform());
  }

  @Test
  @ClearEnvironmentVariable(key = "CONTAINER_APP_NAME")
  @ClearEnvironmentVariable(key = "FUNCTIONS_EXTENSION_VERSION")
  @SetEnvironmentVariable(key = "WEBSITE_SITE_NAME", value = "my-app")
  void detectPlatformreturnsAppServicewhenOnlyWebsiteSiteNameSet() {
    assertEquals("APP_SERVICE", AzureResourceReader.detectPlatform());
  }

  @Test
  @ClearEnvironmentVariable(key = "CONTAINER_APP_NAME")
  @SetEnvironmentVariable(key = "WEBSITE_SITE_NAME", value = "my-func")
  @SetEnvironmentVariable(key = "FUNCTIONS_EXTENSION_VERSION", value = "~4")
  void detectPlatformreturnsFunctionswhenFunctionsExtensionVersionSet() {
    assertEquals("FUNCTIONS", AzureResourceReader.detectPlatform());
  }

  @Test
  @SetEnvironmentVariable(key = "CONTAINER_APP_NAME", value = "my-container")
  void detectPlatformreturnsContainerAppwhenContainerAppNameSet() {
    assertEquals("CONTAINER_APP", AzureResourceReader.detectPlatform());
  }

  @Test
  @ClearEnvironmentVariable(key = "CONTAINER_APP_NAME")
  @ClearEnvironmentVariable(key = "FUNCTIONS_EXTENSION_VERSION")
  @SetEnvironmentVariable(key = "WEBSITE_SITE_NAME", value = "my-app")
  void detectAttributesreturnsAppServiceAttributeswhenWebsiteSiteNameSet() {
    Map<String, String> attributes = AzureResourceReader.detectAttributes();

    assertEquals("azure", attributes.get("cloud.provider"));
    assertEquals("azure.app_service", attributes.get("cloud.platform"));
    assertEquals("my-app", attributes.get("service.name"));
  }

  @Test
  @ClearEnvironmentVariable(key = "CONTAINER_APP_NAME")
  @SetEnvironmentVariable(key = "WEBSITE_SITE_NAME", value = "my-func")
  @SetEnvironmentVariable(key = "FUNCTIONS_EXTENSION_VERSION", value = "~4")
  void detectAttributesreturnsFunctionsAttributeswhenFunctionsExtensionVersionSet() {
    Map<String, String> attributes = AzureResourceReader.detectAttributes();

    assertEquals("azure", attributes.get("cloud.provider"));
    assertEquals("azure.functions", attributes.get("cloud.platform"));
    assertEquals("my-func", attributes.get("faas.name"));
    assertEquals("~4", attributes.get("faas.version"));
  }

  @Test
  @SetEnvironmentVariable(key = "CONTAINER_APP_NAME", value = "my-container")
  @SetEnvironmentVariable(key = "CONTAINER_APP_REPLICA_NAME", value = "replica-001")
  @SetEnvironmentVariable(key = "CONTAINER_APP_REVISION", value = "v1")
  void detectAttributesreturnsContainerAppAttributeswhenContainerAppNameSet() {
    Map<String, String> attributes = AzureResourceReader.detectAttributes();

    assertEquals("azure", attributes.get("cloud.provider"));
    assertEquals("azure.container_apps", attributes.get("cloud.platform"));
    assertEquals("my-container", attributes.get("service.name"));
    assertEquals("replica-001", attributes.get("service.instance.id"));
    assertEquals("v1", attributes.get("service.version"));
  }

  @Test
  @ClearEnvironmentVariable(key = "CONTAINER_APP_NAME")
  @ClearEnvironmentVariable(key = "FUNCTIONS_EXTENSION_VERSION")
  @SetEnvironmentVariable(key = "WEBSITE_SITE_NAME", value = "my-app")
  @SetEnvironmentVariable(key = "WEBSITE_INSTANCE_ID", value = "instance-123")
  @SetEnvironmentVariable(key = "REGION_NAME", value = "eastus")
  void detectAttributesincludesRegionAndInstanceIdwhenEnvVarsSet() {
    Map<String, String> attributes = AzureResourceReader.detectAttributes();

    assertEquals("eastus", attributes.get("cloud.region"));
    assertEquals("instance-123", attributes.get("service.instance.id"));
    assertTrue(attributes.containsKey("cloud.platform"));
  }
}
