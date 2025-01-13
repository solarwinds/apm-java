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

dependencies {
  compileOnly(project(":bootstrap"))
  compileOnly("org.projectlombok:lombok")
  annotationProcessor("org.projectlombok:lombok")

  compileOnly("com.solarwinds.joboe:config")
  compileOnly("com.solarwinds.joboe:logging")
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv-incubating")

  compileOnly("com.solarwinds.joboe:sampling")
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

  implementation("org.json:json")
  implementation("com.google.code.gson:gson")
  implementation("com.github.ben-manes.caffeine:caffeine")

  testImplementation("org.json:json")
  testImplementation("com.solarwinds.joboe:sampling")
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

swoJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_1_8)
}
