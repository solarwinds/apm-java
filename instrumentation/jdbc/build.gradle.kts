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
  id("solarwinds.instrumentation-conventions")
}

dependencies {
  compileOnly(project(":bootstrap"))
  compileOnly("com.solarwinds.joboe:config")
  implementation(project(":instrumentation:instrumentation-shared"))

  compileOnly("org.json:json")
  compileOnly("com.solarwinds.joboe:logging")
  compileOnly("io.opentelemetry:opentelemetry-sdk-trace")

  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv")
  compileOnly("com.github.ben-manes.caffeine:caffeine")

  testImplementation(project(":instrumentation:jdbc"))
  testImplementation(project(":instrumentation:instrumentation-shared"))
  testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.3"))

  testImplementation("org.testcontainers:mysql")
  testImplementation("org.testcontainers:junit-jupiter")
  testImplementation("com.mysql:mysql-connector-j:9.2.0")
}

tasks.withType<JavaCompile>().configureEach {
  dependsOn(":instrumentation:instrumentation-shared:byteBuddyJava")
}

swoJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_1_8)
}
