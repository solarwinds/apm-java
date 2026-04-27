import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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

plugins {
  id("solarwinds.java-conventions")
}

dependencies {
  compileOnly(project(":bootstrap"))

  compileOnly(project(":libs:config"))
  compileOnly(project(":libs:logging"))
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv-incubating")

  compileOnly(project(":libs:sampling"))
  compileOnly("com.google.auto.service:auto-service")
  annotationProcessor("com.google.auto.service:auto-service")

  compileOnly("io.opentelemetry:opentelemetry-api")
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  implementation("io.opentelemetry.contrib:opentelemetry-span-stacktrace") {
    exclude(module = "opentelemetry-sdk", group = "io.opentelemetry") // the agent includes this
  }

  implementation("io.opentelemetry.contrib:opentelemetry-cel-sampler") {
    exclude(module = "opentelemetry-sdk", group = "io.opentelemetry") // the agent includes this
    exclude(group = "com.google.code.findbugs", module = "annotations")
  }

  implementation("org.json:json")
  implementation("com.google.code.gson:gson")
  implementation("com.github.ben-manes.caffeine:caffeine")

  compileOnly("io.opentelemetry:opentelemetry-api-incubator")
  compileOnly("io.opentelemetry:opentelemetry-exporter-otlp")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-incubator")

  testImplementation("org.json:json")
  testImplementation(project(":libs:config"))
  testImplementation(project(":libs:logging"))
  testImplementation(project(":libs:sampling"))
  testImplementation("io.opentelemetry:opentelemetry-exporter-otlp")

  testImplementation("io.opentelemetry:opentelemetry-api-incubator")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator")
}

val otelAgentVersion: String by rootProject.extra
val swoAgentVersion: String by rootProject.extra

buildConfig {
  val currentDateTime = ZonedDateTime.now()
  val formatter = DateTimeFormatter.ofPattern("LLL dd, yyyy - HH:mm:ss z")
  val formattedDate = currentDateTime.format(formatter)

  packageName("com.solarwinds.opentelemetry.extensions")
  buildConfigField("String", "OTEL_AGENT_VERSION", "\"${otelAgentVersion}\"")
  buildConfigField("String", "SOLARWINDS_AGENT_VERSION", "\"${swoAgentVersion}\"")

  buildConfigField("String", "BUILD_DATETIME", "\"$formattedDate\"")
}

tasks.named<JavaCompile>("compileJava") {
  // Disable AutoService verify check to prevent rawtypes warnings for generic service provider interfaces
  options.compilerArgs.add("-Averify=false")
}

swoJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_1_8)
}
