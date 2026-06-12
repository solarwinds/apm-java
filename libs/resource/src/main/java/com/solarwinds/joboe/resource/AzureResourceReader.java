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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.azure.resource.AzureEnvVarPlatform;
import io.opentelemetry.contrib.azure.resource.AzureResourceDetector;
import io.opentelemetry.sdk.resources.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * Facade over the OpenTelemetry {@code azure-resources} detector.
 *
 * <p>This class is the single point that touches the OpenTelemetry SDK autoconfigure SPI. All
 * OpenTelemetry types referenced here are relocated into a private namespace when this module is
 * shaded, so callers receive only plain JDK types and never link against the SPI classes that are
 * unavailable in the joboe core bootstrap class loader.
 */
public final class AzureResourceReader {

  private AzureResourceReader() {}

  /**
   * Detects the Azure platform from environment variables.
   *
   * @return the platform name, one of {@code APP_SERVICE}, {@code FUNCTIONS}, {@code CONTAINER_APP}
   *     or {@code NONE}
   */
  public static String detectPlatform() {
    return AzureEnvVarPlatform.detect(System.getenv()).name();
  }

  /**
   * Runs the Azure resource detector and flattens the detected resource into a string map keyed by
   * OpenTelemetry attribute key.
   *
   * <p>This may perform an HTTP call to the Azure instance metadata service for the plain VM case.
   * Callers are responsible for suppressing instrumentation around this call.
   *
   * @return a map of attribute key to value; empty if nothing is detected
   */
  public static Map<String, String> detectAttributes() {
    Resource resource = new AzureResourceDetector().create(DeclarativeConfigProperties.empty());
    Attributes attributes = resource.getAttributes();
    Map<String, String> result = new HashMap<>(attributes.size());
    attributes.forEach((key, value) -> result.put(key.getKey(), String.valueOf(value)));
    return result;
  }
}
