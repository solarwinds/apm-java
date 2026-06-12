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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("solarwinds.java-conventions")
  id("com.gradleup.shadow")
}

description = "shaded azure resource detection"

// The OpenTelemetry azure-resources detector is only consumable through the SDK
// autoconfigure SPI (ComponentProvider / ConditionalResourceProvider). Those SPI
// classes are not resolvable from the joboe core bootstrap class loader at premain.
// To use the detector from there, we bundle it together with the OpenTelemetry SDK
// it depends on and relocate everything into a private namespace so it is fully
// self-contained and cannot collide with the agent's own (unrelocated) SDK that
// lives in the agent class loader.
dependencies {
  implementation("io.opentelemetry.contrib:opentelemetry-azure-resources")
  implementation("io.opentelemetry:opentelemetry-sdk")
  implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  implementation("io.opentelemetry:opentelemetry-api")
  implementation("io.opentelemetry:opentelemetry-api-incubator")
  implementation("io.opentelemetry.semconv:opentelemetry-semconv")
  implementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating")
}

tasks.withType<Test>().configureEach {
  jvmArgs(
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.util=ALL-UNNAMED"
  )
}

tasks.withType<ShadowJar>().configureEach {
  mergeServiceFiles()
  // Relocate the bundled OpenTelemetry SDK + azure detector and its Jackson
  // dependency into a private namespace. The public facade in
  // com.solarwinds.joboe.resource is intentionally left unrelocated.
  relocate("io.opentelemetry", "com.solarwinds.joboe.shaded.azure.io.opentelemetry")
  relocate("com.fasterxml.jackson", "com.solarwinds.joboe.shaded.azure.com.fasterxml.jackson")
  exclude("**/module-info.class")
}
