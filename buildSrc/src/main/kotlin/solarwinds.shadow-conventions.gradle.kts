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
  id("com.gradleup.shadow")
}

tasks.withType<ShadowJar>().configureEach {
  // Prevents conflict with other SLF4J instances. Important for premain.
  relocate("org.slf4j", "io.opentelemetry.javaagent.slf4j")

  // prevents conflict with library instrumentation, since these classes live in the bootstrap class loader
  relocate("io.opentelemetry.instrumentation", "io.opentelemetry.javaagent.shaded.instrumentation") {
    // Exclude resource providers since they live in the agent class loader
    exclude("io.opentelemetry.instrumentation.resources.*")
    exclude("io.opentelemetry.instrumentation.spring.resources.*")
  }

  // relocate OpenTelemetry API usage
  relocate( "io.opentelemetry.api", "io.opentelemetry.javaagent.shaded.io.opentelemetry.api")
  relocate ("io.opentelemetry.semconv", "io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv")
  relocate ("io.opentelemetry.context", "io.opentelemetry.javaagent.shaded.io.opentelemetry.context")

  // relocate the OpenTelemetry extensions that are used by instrumentation modules
  // these extensions live in the AgentClassLoader, and are injected into the user"s class loader
  // by the instrumentation modules that use them
  relocate ("io.opentelemetry.extension.aws", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.aws")
  relocate ("io.opentelemetry.extension.kotlin", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.kotlin")

  relocate("com.google", "com.solarwinds.joboe.shaded.google")
  relocate("com.github.benmanes", "com.solarwinds.joboe.shaded.caffeine")
  relocate("org.checkerframework", "com.solarwinds.joboe.shaded.checkerframework")
  relocate("org.json", "com.solarwinds.joboe.shaded.org.json")
}