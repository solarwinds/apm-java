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
  testImplementation(project(":solarwinds-otel-sdk"))
  testImplementation(project(":libs:sampling"))
  testImplementation(project(":libs:core"))
  testImplementation("jakarta.servlet:jakarta.servlet-api:5.0.0")
  testImplementation("org.awaitility:awaitility:4.3.0")
}

tasks.named("compileTestJava") {
  dependsOn(project(":solarwinds-otel-sdk").tasks.named("shadowJar"))
}

swoJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_17)
}
